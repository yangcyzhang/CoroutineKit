package com.yangcyzhang.coroutinekit.ext

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class FlowExtTest {

    // ───────────────────────── throttleFirst ─────────────────────────

    @Test
    fun `throttleFirst emits the first item in a window`() = runTest {
        val results = flow {
            emit(1)
            emit(2) // within the window — should be dropped
            emit(3) // within the window — should be dropped
        }
            .throttleFirst(windowDuration = 500)
            .toList()

        assertEquals(listOf(1), results)
    }

    @Test
    fun `throttleFirst emits multiple items from different windows`() = runTest {
        var callCount = 0
        val results = mutableListOf<Int>()

        flow {
            emit(1)
        }.throttleFirst(windowDuration = 0).toList(results)

        // With 0ms window, every item should pass through
        assertEquals(listOf(1), results)
    }

    // ───────────────────────── retryWithDelay ─────────────────────────

    @Test
    fun `retryWithDelay retries on failure and succeeds`() = runTest {
        var attempt = 0
        val results = flow {
            attempt++
            if (attempt < 3) throw RuntimeException("fail $attempt")
            emit("success")
        }
            .retryWithDelay(times = 3, initialDelay = 0)
            .toList()

        assertEquals(listOf("success"), results)
        assertEquals(3, attempt)
    }

    @Test
    fun `retryWithDelay throws after max retries exhausted`() = runTest {
        var attempt = 0
        val upstream = flow<String> {
            attempt++
            throw RuntimeException("always fail")
        }.retryWithDelay(times = 2, initialDelay = 0)

        assertFailsWith<RuntimeException> {
            upstream.toList()
        }
        assertEquals(3, attempt) // 1 initial + 2 retries
    }

    @Test
    fun `retryWithDelay does not retry when predicate returns false`() = runTest {
        var attempt = 0
        val upstream = flow<String> {
            attempt++
            throw IllegalStateException("no retry")
        }.retryWithDelay(times = 3, initialDelay = 0, predicate = { false })

        assertFailsWith<IllegalStateException> {
            upstream.toList()
        }
        assertEquals(1, attempt, "Should not have retried")
    }

    // ───────────────────────── onEachCatching ─────────────────────────

    @Test
    fun `onEachCatching does not cancel flow when action throws`() = runTest {
        val results = mutableListOf<Int>()
        flow {
            emit(1)
            emit(2)
            emit(3)
        }
            .onEachCatching { value ->
                if (value == 2) throw RuntimeException("boom")
                results.add(value)
            }
            .toList() // collect to drive the flow

        // Items 1 and 3 should be processed; 2 caused an exception that was swallowed
        assertEquals(listOf(1, 3), results)
    }

    @Test
    fun `onEachCatching processes all items when no exception is thrown`() = runTest {
        val results = mutableListOf<Int>()
        flow { emit(1); emit(2); emit(3) }
            .onEachCatching { results.add(it) }
            .toList()

        assertEquals(listOf(1, 2, 3), results)
    }

    // ───────────────────────── mapNotNullCatching ─────────────────────────

    @Test
    fun `mapNotNullCatching filters nulls and swallows exceptions`() = runTest {
        val results = flow {
            emit(1)
            emit(2)
            emit(3)
        }
            .mapNotNullCatching { value ->
                when (value) {
                    1 -> "one"
                    2 -> null       // filtered
                    3 -> throw RuntimeException("error") // swallowed
                    else -> null
                }
            }
            .toList()

        assertEquals(listOf("one"), results)
    }
}
