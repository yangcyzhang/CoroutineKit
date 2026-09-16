package com.yangcyzhang.coroutinekit.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DispatcherUtilsTest {

    @AfterEach
    fun tearDown() {
        CoroutineKitDispatchers.resetToDefaults()
    }

    @Test
    fun `setTestDispatchers replaces all dispatchers with the given test dispatcher`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        CoroutineKitDispatchers.setTestDispatchers(testDispatcher)

        assertEquals(testDispatcher, CoroutineKitDispatchers.io)
        assertEquals(testDispatcher, CoroutineKitDispatchers.default)
        assertEquals(testDispatcher, CoroutineKitDispatchers.unconfined)
    }

    @Test
    fun `resetToDefaults restores io dispatcher to Dispatchers IO`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        CoroutineKitDispatchers.setTestDispatchers(testDispatcher)
        CoroutineKitDispatchers.resetToDefaults()

        assertEquals(Dispatchers.IO, CoroutineKitDispatchers.io)
        assertEquals(Dispatchers.Default, CoroutineKitDispatchers.default)
        assertEquals(Dispatchers.Unconfined, CoroutineKitDispatchers.unconfined)
    }

    @Test
    fun `setTestDispatchers and resetToDefaults do not interfere with each other`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        CoroutineKitDispatchers.setTestDispatchers(testDispatcher)
        assertEquals(testDispatcher, CoroutineKitDispatchers.io)

        CoroutineKitDispatchers.resetToDefaults()
        assertNotEquals(testDispatcher, CoroutineKitDispatchers.io)
    }

    @Test
    fun `withIO executes block and returns result`() = runTest {
        CoroutineKitDispatchers.setTestDispatchers(StandardTestDispatcher(testScheduler))

        val result = withIO { "io result" }
        assertEquals("io result", result)
    }

    @Test
    fun `withDefault executes block and returns result`() = runTest {
        CoroutineKitDispatchers.setTestDispatchers(StandardTestDispatcher(testScheduler))

        val result = withDefault { 99 }
        assertEquals(99, result)
    }
}
