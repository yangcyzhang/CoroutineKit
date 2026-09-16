package com.yangcyzhang.coroutinekit.retry

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class RetryPolicyTest {

    // ───────────────────────── Fixed ─────────────────────────

    @Test
    fun `Fixed policy retries the correct number of times`() = runTest {
        var callCount = 0
        assertFailsWith<RuntimeException> {
            withRetry(policy = RetryPolicy.Fixed(times = 3, delayMs = 0)) {
                callCount++
                throw RuntimeException("fail")
            }
        }
        assertEquals(4, callCount) // 1 initial + 3 retries
    }

    @Test
    fun `Fixed policy succeeds before exhausting retries`() = runTest {
        var callCount = 0
        val result = withRetry(policy = RetryPolicy.Fixed(times = 3, delayMs = 0)) { attempt ->
            callCount++
            if (attempt < 2) throw RuntimeException("not yet")
            "success"
        }
        assertEquals("success", result)
        assertEquals(3, callCount)
    }

    @Test
    fun `Fixed policy waits the correct delay between attempts`() = runTest {
        val policy = RetryPolicy.Fixed(times = 2, delayMs = 500)
        // Verify the delayFor() computation is correct for each attempt
        assertEquals(500L, policy.delayFor(0))
        assertEquals(500L, policy.delayFor(1))

        // Also verify the withRetry actually retries the correct number of times
        var callCount = 0
        assertFailsWith<RuntimeException> {
            withRetry(policy = policy) {
                callCount++
                throw RuntimeException("fail")
            }
        }
        assertEquals(3, callCount) // 1 initial + 2 retries
    }

    // ───────────────────────── ExponentialBackoff ─────────────────────────

    @Test
    fun `ExponentialBackoff delay increases exponentially`() = runTest {
        val recordedDelays = mutableListOf<Long>()
        val policy = RetryPolicy.ExponentialBackoff(
            times = 3,
            initialDelayMs = 100,
            maxDelayMs = 10_000,
            multiplier = 2.0
        )
        // delayFor(0) = 100 * 2^0 = 100
        // delayFor(1) = 100 * 2^1 = 200
        // delayFor(2) = 100 * 2^2 = 400
        assertEquals(100L, policy.delayFor(0))
        assertEquals(200L, policy.delayFor(1))
        assertEquals(400L, policy.delayFor(2))
    }

    @Test
    fun `ExponentialBackoff delay is capped at maxDelayMs`() = runTest {
        val policy = RetryPolicy.ExponentialBackoff(
            times = 10,
            initialDelayMs = 1000,
            maxDelayMs = 3000,
            multiplier = 2.0
        )
        // delayFor(10) = 1000 * 2^10 = 1,024,000 — but capped at 3000
        assertEquals(3000L, policy.delayFor(10))
    }

    // ───────────────────────── Immediate ─────────────────────────

    @Test
    fun `Immediate policy has zero delay between retries`() = runTest {
        val policy = RetryPolicy.Immediate(times = 5)
        for (i in 0 until 5) {
            assertEquals(0L, policy.delayFor(i))
        }
    }

    @Test
    fun `Immediate policy retries specified number of times`() = runTest {
        var callCount = 0
        assertFailsWith<RuntimeException> {
            withRetry(policy = RetryPolicy.Immediate(times = 2)) {
                callCount++
                throw RuntimeException("fail")
            }
        }
        assertEquals(3, callCount) // 1 initial + 2 retries
    }

    // ───────────────────────── withRetry predicate ─────────────────────────

    @Test
    fun `withRetry does not retry when predicate returns false`() = runTest {
        var callCount = 0
        assertFailsWith<IllegalArgumentException> {
            withRetry(
                policy = RetryPolicy.Fixed(times = 5, delayMs = 0),
                predicate = { false }
            ) {
                callCount++
                throw IllegalArgumentException("no retry please")
            }
        }
        assertEquals(1, callCount, "Should not have retried at all")
    }

    @Test
    fun `withRetry succeeds on second attempt with correct result`() = runTest {
        var callCount = 0
        val result = withRetry(policy = RetryPolicy.Fixed(times = 3, delayMs = 0)) {
            callCount++
            if (callCount == 1) throw RuntimeException("first try fails")
            "worked on attempt $callCount"
        }
        assertEquals("worked on attempt 2", result)
    }
}
