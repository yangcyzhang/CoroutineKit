package com.yangcyzhang.coroutinekit.ext

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SuspendExtTest {

    // ───────────────────────── suspendRunCatching ─────────────────────────

    @Test
    fun `suspendRunCatching returns Success on successful operation`() = runTest {
        val result = suspendRunCatching { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `suspendRunCatching returns Failure on exception`() = runTest {
        val result = suspendRunCatching<Int> { throw RuntimeException("network error") }
        assertTrue(result.isFailure)
        assertInstanceOf(RuntimeException::class.java, result.exceptionOrNull())
        assertEquals("network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `suspendRunCatching re-throws CancellationException`() = runTest {
        // CancellationException must NOT be wrapped in Result — it must propagate
        assertFailsWith<CancellationException> {
            suspendRunCatching<Int> { throw CancellationException("cancelled") }
        }
    }

    // ───────────────────────── withTimeoutOrDefault ─────────────────────────

    @Test
    fun `withTimeoutOrDefault returns result when within timeout`() = runTest {
        val result = withTimeoutOrDefault(timeMillis = 5_000, defaultValue = -1) {
            100
        }
        assertEquals(100, result)
    }

    @Test
    fun `withTimeoutOrDefault returns default when timeout exceeded`() = runTest {
        val result = withTimeoutOrDefault(timeMillis = 100, defaultValue = "fallback") {
            // This would take 10 seconds — will exceed the 100ms timeout
            kotlinx.coroutines.delay(10_000)
            "real value"
        }
        assertEquals("fallback", result)
    }

    @Test
    fun `withTimeoutOrDefault propagates parent cancellation`() = runTest {
        var returnedFallback = false
        val job = launch {
            withTimeoutOrDefault(timeMillis = 10_000, defaultValue = Unit) {
                kotlinx.coroutines.awaitCancellation()
            }
            returnedFallback = true
        }

        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertTrue(!returnedFallback)
    }

    // ───────────────────────── retry ─────────────────────────

    @Test
    fun `retry succeeds on the first attempt`() = runTest {
        var callCount = 0
        val result = retry(times = 3) {
            callCount++
            "success"
        }
        assertEquals("success", result)
        assertEquals(1, callCount)
    }

    @Test
    fun `retry retries on failure and succeeds on third attempt`() = runTest {
        var callCount = 0
        val result = retry(times = 3) {
            callCount++
            if (callCount < 3) throw RuntimeException("fail")
            "success after retries"
        }
        assertEquals("success after retries", result)
        assertEquals(3, callCount)
    }

    @Test
    fun `retry throws last exception after all retries are exhausted`() = runTest {
        var callCount = 0
        assertFailsWith<RuntimeException> {
            retry(times = 2) {
                callCount++
                throw RuntimeException("always fail")
            }
        }
        assertEquals(3, callCount) // 1 initial + 2 retries
    }

    @Test
    fun `retry does not retry when predicate returns false`() = runTest {
        var callCount = 0
        assertFailsWith<IllegalArgumentException> {
            retry(times = 3, predicate = { false }) {
                callCount++
                throw IllegalArgumentException("stop immediately")
            }
        }
        assertEquals(1, callCount)
    }

    // ───────────────────────── launchCatching ─────────────────────────

    @Test
    fun `launchCatching calls onError when exception is thrown`() = runTest {
        val errors = mutableListOf<Throwable>()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        scope.launchCatching(onError = { errors.add(it) }) {
            throw RuntimeException("something went wrong")
        }
        testScheduler.advanceUntilIdle()

        assertEquals(1, errors.size)
        assertInstanceOf(RuntimeException::class.java, errors.first())
        scope.cancel()
    }

    @Test
    fun `launchCatching completes normally without calling onError`() = runTest {
        val errors = mutableListOf<Throwable>()
        val results = mutableListOf<String>()
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        scope.launchCatching(onError = { errors.add(it) }) {
            results.add("done")
        }
        testScheduler.advanceUntilIdle()

        assertTrue(errors.isEmpty())
        assertEquals(listOf("done"), results)
        scope.cancel()
    }
}
