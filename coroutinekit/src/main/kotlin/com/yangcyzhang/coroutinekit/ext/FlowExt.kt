package com.yangcyzhang.coroutinekit.ext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transform
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * Flow extension utilities for CoroutineKit.
 *
 * Provides production-ready Flow operators that solve common real-world problems:
 * throttling, retry with backoff, and safe side-effect collection.
 *
 * @author yangcyzhang
 * @since 0.1.0
 */

/**
 * Emits the first item in each time window and drops all subsequent items
 * that arrive within that window. Useful for preventing double-click or
 * debouncing rapid UI events.
 *
 * Example:
 * ```kotlin
 * buttonClickFlow
 *     .throttleFirst(500)
 *     .onEach { handleClick() }
 *     .launchIn(scope)
 * ```
 *
 * @param windowDuration the duration of the throttle window
 * @param unit the time unit of [windowDuration], defaults to milliseconds
 * @return a Flow that emits only the first item per time window
 * @author yangcyzhang
 * @since 0.1.0
 */
fun <T> Flow<T>.throttleFirst(
    windowDuration: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS
): Flow<T> {
    val windowMs = unit.toMillis(windowDuration)
    return flow {
        var lastEmitTime = 0L
        collect { value ->
            val now = System.currentTimeMillis()
            if (now - lastEmitTime >= windowMs) {
                lastEmitTime = now
                emit(value)
            }
        }
    }
}

/**
 * Retries the upstream Flow on failure with exponential backoff delay.
 *
 * This operator will NOT retry if the exception does not satisfy [predicate].
 * The delay between retries grows exponentially: `initialDelay * factor^attempt`,
 * capped at [maxDelay].
 *
 * Example:
 * ```kotlin
 * apiFlow
 *     .retryWithDelay(times = 3, initialDelay = 200, factor = 2.0)
 *     .collect { ... }
 * ```
 *
 * @param times maximum number of retry attempts
 * @param initialDelay delay before the first retry, in milliseconds
 * @param maxDelay maximum delay cap, in milliseconds
 * @param factor exponential multiplier applied to each successive delay
 * @param predicate determines whether to retry for a given throwable
 * @return a Flow that retries with exponential backoff on matching errors
 * @author yangcyzhang
 * @since 0.1.0
 */
fun <T> Flow<T>.retryWithDelay(
    times: Int = 3,
    initialDelay: Long = 100L,
    maxDelay: Long = 1_000L,
    factor: Double = 2.0,
    predicate: suspend (Throwable) -> Boolean = { true }
): Flow<T> = retryWhen { cause, attempt ->
    if (attempt < times && predicate(cause)) {
        val delayMs = min(initialDelay * factor.pow(attempt.toDouble()).toLong(), maxDelay)
        delay(delayMs)
        true
    } else {
        false
    }
}

/**
 * Executes [action] on each emitted value without cancelling the Flow on exception.
 *
 * Unlike [onEach], if the [action] throws, the exception is caught and the Flow
 * continues processing subsequent items. [CancellationException] is always re-thrown.
 * This is useful for safe logging,
 * analytics, or side-effect operations that should not affect the main data stream.
 *
 * Example:
 * ```kotlin
 * dataFlow
 *     .onEachCatching { item ->
 *         analytics.track(item) // if this throws, the flow continues
 *     }
 *     .collect { item -> renderUi(item) }
 * ```
 *
 * @param action the side-effect action to execute on each item
 * @return the original Flow with safe side-effect applied
 * @author yangcyzhang
 * @since 0.1.0
 */
fun <T> Flow<T>.onEachCatching(action: suspend (T) -> Unit): Flow<T> =
    onEach { value ->
        try {
            action(value)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Exceptions from action are intentionally swallowed here.
        }
    }

/**
 * A variant of [flatMapLatest] that catches and ignores exceptions thrown
 * inside the [transform] block. When an exception occurs, no value is emitted
 * for that particular input and the flow continues normally. [CancellationException]
 * is always re-thrown.
 *
 * @param transform a function to transform each upstream value into a new Flow
 * @return a Flow applying flatMapLatest with exception suppression
 * @author yangcyzhang
 * @since 0.1.0
 */
@Suppress("OPT_IN_USAGE")
fun <T, R> Flow<T>.flatMapLatestCatching(
    transform: suspend (T) -> Flow<R>
): Flow<R> = flatMapLatest { value ->
    try {
        transform(value)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        flow { /* emit nothing on error */ }
    }
}

/**
 * Maps each upstream value using [transform], filtering out both null results
 * and any exceptions thrown. Equivalent to combining [mapNotNull] with
 * safe exception handling. [CancellationException] is always re-thrown.
 *
 * @param transform a suspending transform that may return null or throw
 * @return a Flow of non-null successfully transformed values
 * @author yangcyzhang
 * @since 0.1.0
 */
fun <T, R : Any> Flow<T>.mapNotNullCatching(
    transform: suspend (T) -> R?
): Flow<R> = transform { value ->
    try {
        transform(value)?.let { emit(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Skip items that fail transformation
    }
}
