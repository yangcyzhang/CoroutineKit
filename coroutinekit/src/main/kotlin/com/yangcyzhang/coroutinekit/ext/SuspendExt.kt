package com.yangcyzhang.coroutinekit.ext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Suspend function extension utilities for CoroutineKit.
 *
 * Provides safer, more ergonomic wrappers around common suspend patterns:
 * exception handling, timeouts, retries, and safe coroutine launching.
 *
 * @author yangcyzhang
 * @since 0.1.0
 */

/**
 * Executes the given [block] as a suspending operation and wraps the result in [Result].
 *
 * Unlike the standard `runCatching`, this function **re-throws** [CancellationException]
 * to ensure structured concurrency is not violated. Accidentally swallowing
 * [CancellationException] is a common and dangerous mistake in coroutine code.
 *
 * Example:
 * ```kotlin
 * val result = suspendRunCatching { repository.fetchUser(id) }
 * result.onSuccess { render(it) }.onFailure { showError(it) }
 * ```
 *
 * @param block the suspending operation to execute
 * @return [Result.success] with the value, or [Result.failure] with the exception.
 *         [CancellationException] is always re-thrown, never wrapped in Result.
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e // Must re-throw — do NOT swallow CancellationException
} catch (e: Exception) {
    Result.failure(e)
}

/**
 * Executes the given [block] within the specified timeout. If the timeout is exceeded,
 * returns [defaultValue] instead of throwing [kotlinx.coroutines.TimeoutCancellationException].
 * Cancellation initiated by an outer coroutine scope is always propagated.
 *
 * This is a safer alternative to [withTimeout] when a fallback value is preferred
 * over an exception-based control flow.
 *
 * Example:
 * ```kotlin
 * val feed = withTimeoutOrDefault(timeMillis = 3_000, defaultValue = emptyList()) {
 *     networkService.fetchFeed()
 * }
 * ```
 *
 * @param timeMillis the timeout in milliseconds
 * @param defaultValue the value to return if the timeout is exceeded
 * @param block the suspending operation to execute
 * @return the result of [block], or [defaultValue] if the timeout is exceeded
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> withTimeoutOrDefault(
    timeMillis: Long,
    defaultValue: T,
    block: suspend CoroutineScope.() -> T
): T = try {
    withTimeout(timeMillis, block)
} catch (_: TimeoutCancellationException) {
    defaultValue
}

/**
 * A simple suspending retry loop that retries [block] up to [times] times.
 *
 * The [attempt] parameter (0-indexed) is passed into the block so callers
 * can adapt their behavior across attempts (e.g., logging, changing parameters).
 *
 * [CancellationException] is never retried — it propagates immediately.
 * If [predicate] returns false for a given exception, retrying stops and
 * the exception is thrown immediately.
 *
 * Example:
 * ```kotlin
 * val user = retry(times = 3, delay = 500) { attempt ->
 *     println("Attempt $attempt")
 *     api.getUser(id)
 * }
 * ```
 *
 * @param times maximum number of retry attempts (total calls = times + 1)
 * @param delay delay in milliseconds between attempts (0 = no delay)
 * @param predicate determines whether to retry for a given exception
 * @param block the suspending operation, receives the current attempt index
 * @return the successful result of [block]
 * @throws Exception the last thrown exception when all retries are exhausted
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> retry(
    times: Int = 3,
    delay: Long = 0L,
    predicate: (Throwable) -> Boolean = { true },
    block: suspend (attempt: Int) -> T
): T {
    var lastException: Exception? = null
    repeat(times + 1) { attempt ->
        try {
            return block(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!predicate(e)) throw e
            lastException = e
            if (attempt < times && delay > 0) {
                delay(delay)
            }
        }
    }
    throw lastException!!
}

/**
 * Launches a coroutine that safely catches any uncaught exception and
 * delivers it to [onError] instead of crashing or silently disappearing.
 *
 * This is useful in ViewModels or repositories where you want to handle
 * errors in a centralized way without setting up a full [CoroutineExceptionHandler].
 *
 * Example:
 * ```kotlin
 * scope.launchCatching(onError = { e -> _errorState.value = e.message }) {
 *     val data = repository.fetch()
 *     _uiState.value = data
 * }
 * ```
 *
 * @param context additional coroutine context elements
 * @param onError called on the coroutine's thread when an exception is thrown
 * @param block the coroutine body
 * @return the [Job] of the launched coroutine
 * @author yangcyzhang
 * @since 0.1.0
 */
fun CoroutineScope.launchCatching(
    context: CoroutineContext = EmptyCoroutineContext,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job = launch(context) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onError(e)
    }
}
