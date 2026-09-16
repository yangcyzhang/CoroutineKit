package com.yangcyzhang.coroutinekit.interop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.future.future
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Java interoperability bridges for CoroutineKit.
 *
 * Provides utilities to bridge between Kotlin's coroutine/Flow world and
 * Java's callback/Future-based APIs. This is essential for:
 * - Exposing suspend functions to Java callers
 * - Wrapping legacy callback-based Android/JVM SDKs into reactive Flows
 *
 * @author yangcyzhang
 * @since 0.1.0
 */

/**
 * Bridges a suspending [block] into a [CompletableFuture], making it callable
 * from Java without requiring Kotlin's `Continuation` parameter.
 *
 * The coroutine runs in a [CoroutineScope] launched with [Dispatchers.IO] by default.
 * If the coroutine fails, the future completes exceptionally.
 *
 * Example (Kotlin):
 * ```kotlin
 * val future: CompletableFuture<User> = suspendToFuture { repository.fetchUser(id) }
 * ```
 *
 * Example (Java):
 * ```java
 * CompletableFuture<User> future = JavaInteropKt.suspendToFuture(() -> ...);
 * future.thenAccept(user -> render(user));
 * ```
 *
 * @param scope the [CoroutineScope] to launch the coroutine in
 * @param block the suspending operation to execute
 * @return a [CompletableFuture] that completes with the result of [block]
 * @author yangcyzhang
 * @since 0.1.0
 */
fun <T> suspendToFuture(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    block: suspend () -> T
): CompletableFuture<T> = scope.future { block() }

/**
 * Converts a callback-based API into a [Flow], automatically managing
 * registration and unregistration lifecycle.
 *
 * The [register] lambda is called when the Flow starts being collected.
 * The [unregister] lambda is called when the collector's coroutine is cancelled
 * or the Flow completes, preventing memory leaks.
 *
 * Example:
 * ```kotlin
 * val locationFlow: Flow<Location> = callbackToFlow(
 *     register = { onResult, onError ->
 *         locationManager.requestUpdates(object : LocationListener {
 *             override fun onLocation(loc: Location) = onResult(loc)
 *             override fun onError(e: Exception) = onError(e)
 *         })
 *     },
 *     unregister = { locationManager.removeUpdates() }
 * )
 * ```
 *
 * @param register a function that registers the callback; receives `onResult` and `onError`
 * @param unregister a function to clean up the callback registration
 * @return a cold [Flow] that emits values delivered through the callback
 * @author yangcyzhang
 * @since 0.1.0
 */
fun <T> callbackToFlow(
    register: (onResult: (T) -> Unit, onError: (Throwable) -> Unit) -> Unit,
    unregister: () -> Unit
): Flow<T> = callbackFlow {
    register(
        { value -> trySend(value) },
        { error -> close(error) }
    )
    awaitClose { unregister() }
}

/**
 * Converts a one-shot callback-based API into a suspending function.
 *
 * This is useful for APIs that call a single callback exactly once
 * (e.g., one-time auth results, single permission responses).
 *
 * Example:
 * ```kotlin
 * suspend fun requestPermission(): Boolean = singleCallbackToSuspend { callback ->
 *     permissionManager.request { granted -> callback(granted) }
 * }
 * ```
 *
 * @param register a function that registers the one-shot callback
 * @return a suspending function that awaits the callback result
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> singleCallbackToSuspend(
    register: (callback: (T) -> Unit) -> Unit
): T = suspendCancellableCoroutine { continuation ->
    register { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
}

/**
 * Converts a one-shot callback-based API that may fail into a suspending function.
 *
 * Similar to [singleCallbackToSuspend] but also handles error callbacks.
 *
 * @param register a function that registers callbacks for success and failure
 * @return a suspending function that awaits the callback result or throws on error
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> singleCallbackToSuspendResult(
    register: (onSuccess: (T) -> Unit, onError: (Throwable) -> Unit) -> Unit
): T = suspendCancellableCoroutine { continuation ->
    register(
        { result -> if (continuation.isActive) continuation.resume(result) },
        { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    )
}
