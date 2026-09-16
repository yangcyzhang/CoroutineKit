package com.yangcyzhang.coroutinekit.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Defines a strategy for how a failed operation should be retried.
 *
 * Use [withRetry] to execute a suspending block with a given policy.
 *
 * Example:
 * ```kotlin
 * val result = withRetry(
 *     policy = RetryPolicy.ExponentialBackoff(times = 4, initialDelayMs = 100)
 * ) { attempt ->
 *     println("Attempt: $attempt")
 *     networkCall()
 * }
 * ```
 *
 * @author yangcyzhang
 * @since 0.1.0
 */
sealed class RetryPolicy {

    /**
     * Retries the operation [times] times with a fixed [delayMs] between each attempt.
     *
     * @param times number of retry attempts (total calls = times + 1)
     * @param delayMs milliseconds to wait between each retry
     */
    data class Fixed(
        val times: Int,
        val delayMs: Long
    ) : RetryPolicy()

    /**
     * Retries the operation using exponential backoff.
     *
     * The delay for attempt `n` is: `min(initialDelayMs * multiplier^n, maxDelayMs)`
     *
     * @param times number of retry attempts (total calls = times + 1)
     * @param initialDelayMs delay before the first retry
     * @param maxDelayMs upper cap on the computed delay
     * @param multiplier growth factor applied per attempt
     */
    data class ExponentialBackoff(
        val times: Int,
        val initialDelayMs: Long = 100L,
        val maxDelayMs: Long = 10_000L,
        val multiplier: Double = 2.0
    ) : RetryPolicy()

    /**
     * Retries the operation [times] times immediately with no delay.
     *
     * @param times number of retry attempts (total calls = times + 1)
     */
    data class Immediate(
        val times: Int
    ) : RetryPolicy()

    /**
     * Returns the maximum number of retry attempts for this policy.
     */
    internal val maxRetries: Int
        get() = when (this) {
            is Fixed -> times
            is ExponentialBackoff -> times
            is Immediate -> times
        }

    /**
     * Computes the delay in milliseconds before the given attempt index.
     *
     * @param attempt the 0-based attempt index
     * @return delay in milliseconds
     */
    internal fun delayFor(attempt: Int): Long = when (this) {
        is Fixed -> delayMs
        is ExponentialBackoff -> min(
            (initialDelayMs * multiplier.pow(attempt.toDouble())).toLong(),
            maxDelayMs
        )
        is Immediate -> 0L
    }
}

/**
 * Executes [block] with the given [RetryPolicy], retrying on failures.
 *
 * [CancellationException] is never retried and always propagates.
 * If [predicate] returns `false` for a given throwable, the exception is
 * re-thrown immediately without retrying.
 *
 * Example:
 * ```kotlin
 * // Retry with exponential backoff, only on network errors
 * val response = withRetry(
 *     policy = RetryPolicy.ExponentialBackoff(times = 3),
 *     predicate = { it is IOException }
 * ) { attempt ->
 *     api.fetchData()
 * }
 * ```
 *
 * @param policy the [RetryPolicy] that governs retry timing and count
 * @param predicate determines whether to retry for a given exception; defaults to always retry
 * @param block the suspending operation to execute; receives 0-based attempt index
 * @return the successful result of [block]
 * @throws Exception the last thrown exception when all retries are exhausted
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> withRetry(
    policy: RetryPolicy,
    predicate: (Throwable) -> Boolean = { true },
    block: suspend (attempt: Int) -> T
): T {
    var lastException: Exception? = null
    val maxRetries = policy.maxRetries

    for (attempt in 0..maxRetries) {
        try {
            return block(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!predicate(e)) throw e
            lastException = e
            if (attempt < maxRetries) {
                val delayMs = policy.delayFor(attempt)
                if (delayMs > 0) delay(delayMs)
            }
        }
    }
    throw lastException!!
}
