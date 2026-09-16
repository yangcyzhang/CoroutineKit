package com.yangcyzhang.coroutinekit.lifecycle

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.coroutines.CoroutineContext

/**
 * A lifecycle-aware [CoroutineScope] that uses [SupervisorJob] to ensure
 * that a single child coroutine failure does not propagate to siblings or
 * cancel the entire scope.
 *
 * Implements [Closeable] so it integrates naturally with `use {}` blocks and
 * resource management patterns. Always call [close] when the owning component
 * is destroyed to prevent coroutine leaks.
 *
 * Example:
 * ```kotlin
 * class MyRepository : Closeable {
 *     private val scope = ManagedScope(Dispatchers.IO)
 *
 *     fun startSync() = scope.launch {
 *         while (isActive) {
 *             sync()
 *             delay(30_000)
 *         }
 *     }
 *
 *     override fun close() = scope.close()
 * }
 * ```
 *
 * @param dispatcher the [CoroutineDispatcher] to use for coroutines in this scope
 * @author yangcyzhang
 * @since 0.1.0
 */
class ManagedScope(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope, Closeable {

    private val supervisorJob = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        // Log or handle uncaught exceptions from child coroutines.
        // By default, we print to stderr. Override by passing a custom handler.
        System.err.println("[CoroutineKit/ManagedScope] Uncaught exception: $throwable")
    }

    override val coroutineContext: CoroutineContext =
        supervisorJob + dispatcher + exceptionHandler

    /**
     * Returns `true` if the scope has not been cancelled/closed.
     */
    val isActive: Boolean
        get() = supervisorJob.isActive

    /**
     * Launches a new coroutine in this scope.
     *
     * @param block the coroutine body
     * @return the [Job] of the launched coroutine
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        (this as CoroutineScope).launch(block = block)

    /**
     * Launches a new async coroutine in this scope, returning a [Deferred] result.
     *
     * @param block the coroutine body
     * @return [Deferred] holding the result
     */
    fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> =
        (this as CoroutineScope).async(block = block)

    /**
     * Cancels all coroutines in this scope and releases resources.
     * After calling this, the scope is no longer usable.
     *
     * This should be called when the owning component (e.g., ViewModel, Service,
     * Repository) is destroyed.
     */
    override fun close() {
        cancel()
    }
}

/**
 * Factory function to create a [ManagedScope] with an optional dispatcher.
 *
 * Example:
 * ```kotlin
 * val scope = managedScope(Dispatchers.IO)
 * ```
 *
 * @param dispatcher the dispatcher for the scope, defaults to [Dispatchers.Default]
 * @return a new [ManagedScope]
 * @author yangcyzhang
 * @since 0.1.0
 */
fun managedScope(dispatcher: CoroutineDispatcher = Dispatchers.Default): ManagedScope =
    ManagedScope(dispatcher)
