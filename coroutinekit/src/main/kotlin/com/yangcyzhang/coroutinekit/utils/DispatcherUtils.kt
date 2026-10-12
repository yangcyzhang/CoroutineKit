package com.yangcyzhang.coroutinekit.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Centralized, test-mockable coroutine dispatcher provider.
 *
 * Instead of referencing `Dispatchers.IO` directly in your code, use
 * [CoroutineKitDispatchers.io]. In tests, call [setTestDispatchers] to
 * replace all dispatchers with a [TestCoroutineDispatcher], making
 * coroutines run synchronously and deterministically.
 *
 * Example (production code):
 * ```kotlin
 * suspend fun fetchData() = withIO { repository.fetch() }
 * ```
 *
 * Example (test setup):
 * ```kotlin
 * @BeforeEach
 * fun setup() {
 *     CoroutineKitDispatchers.setTestDispatchers(StandardTestDispatcher())
 * }
 *
 * @AfterEach
 * fun tearDown() {
 *     CoroutineKitDispatchers.resetToDefaults()
 * }
 * ```
 *
 * @author yangcyzhang
 * @since 0.1.0
 */
object CoroutineKitDispatchers {

    /**
     * Dispatcher for main thread / UI operations.
     * Defaults to [Dispatchers.Default] because this artifact also supports plain JVM.
     */
    var main: CoroutineDispatcher = Dispatchers.Default
        private set

    /**
     * Dispatcher for blocking I/O operations (network, disk).
     * Defaults to [Dispatchers.IO].
     */
    var io: CoroutineDispatcher = Dispatchers.IO
        private set

    /**
     * Dispatcher for CPU-intensive computations.
     * Defaults to [Dispatchers.Default].
     */
    var default: CoroutineDispatcher = Dispatchers.Default
        private set

    /**
     * Unconfined dispatcher (runs in the caller's thread until first suspension).
     * Defaults to [Dispatchers.Unconfined].
     */
    var unconfined: CoroutineDispatcher = Dispatchers.Unconfined
        private set

    /**
     * Overrides all dispatchers with the given [dispatcher].
     * Useful in unit tests to make all coroutines use a [TestCoroutineDispatcher].
     *
     * @param dispatcher the dispatcher to set for all contexts
     */
    fun setTestDispatchers(dispatcher: CoroutineDispatcher) {
        main = dispatcher
        io = dispatcher
        default = dispatcher
        unconfined = dispatcher
    }

    /**
     * Restores all dispatchers to their production defaults.
     * Should be called in test tearDown methods.
     */
    fun resetToDefaults() {
        main = Dispatchers.Default  // Note: Dispatchers.Main requires Android runtime
        io = Dispatchers.IO
        default = Dispatchers.Default
        unconfined = Dispatchers.Unconfined
    }
}

/**
 * Shorthand for `withContext(CoroutineKitDispatchers.io)`.
 *
 * Use this instead of `withContext(Dispatchers.IO)` directly so that
 * the dispatcher can be swapped out in tests.
 *
 * @param block the suspending block to execute on the IO dispatcher
 * @return the result of [block]
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> withIO(block: suspend CoroutineScope.() -> T): T =
    withContext(CoroutineKitDispatchers.io, block)

/**
 * Shorthand for `withContext(CoroutineKitDispatchers.default)`.
 *
 * Use this instead of `withContext(Dispatchers.Default)` directly so that
 * the dispatcher can be swapped out in tests.
 *
 * @param block the suspending block to execute on the Default dispatcher
 * @return the result of [block]
 * @author yangcyzhang
 * @since 0.1.0
 */
suspend fun <T> withDefault(block: suspend CoroutineScope.() -> T): T =
    withContext(CoroutineKitDispatchers.default, block)
