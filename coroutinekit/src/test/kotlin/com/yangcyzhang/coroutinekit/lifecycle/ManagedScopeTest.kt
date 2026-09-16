package com.yangcyzhang.coroutinekit.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagedScopeTest {

    @Test
    fun `launched coroutine runs and completes successfully`() = runTest {
        val scope = ManagedScope(StandardTestDispatcher(testScheduler))
        val results = mutableListOf<String>()

        scope.launch { results.add("hello") }
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("hello"), results)
        scope.close()
    }

    @Test
    fun `async deferred returns the correct value`() = runTest {
        val scope = ManagedScope(StandardTestDispatcher(testScheduler))

        val deferred = scope.async { 42 }
        testScheduler.advanceUntilIdle()

        assertEquals(42, deferred.await())
        scope.close()
    }

    @Test
    fun `isActive is true before close`() {
        val scope = ManagedScope(Dispatchers.Default)
        assertTrue(scope.isActive)
        scope.close()
    }

    @Test
    fun `isActive is false after close`() {
        val scope = ManagedScope(Dispatchers.Default)
        scope.close()
        assertFalse(scope.isActive)
    }

    @Test
    fun `close cancels all running coroutines`() = runTest {
        val scope = ManagedScope(StandardTestDispatcher(testScheduler))
        var wasCompleted = false

        scope.launch {
            delay(10_000) // Long-running task
            wasCompleted = true
        }

        scope.close() // Cancel before delay completes
        testScheduler.advanceUntilIdle()

        assertFalse(wasCompleted, "Coroutine should have been cancelled before completing")
    }

    @Test
    fun `child failure does not cancel sibling coroutines (SupervisorJob)`() = runTest {
        val scope = ManagedScope(StandardTestDispatcher(testScheduler))
        var sibling1Completed = false
        var sibling2Completed = false

        scope.launch {
            sibling1Completed = true
        }
        scope.launch {
            throw RuntimeException("I failed") // This should NOT cancel sibling
        }
        scope.launch {
            sibling2Completed = true
        }

        testScheduler.advanceUntilIdle()

        assertTrue(sibling1Completed, "Sibling 1 should have completed despite other child failing")
        assertTrue(sibling2Completed, "Sibling 2 should have completed despite other child failing")
        scope.close()
    }
}
