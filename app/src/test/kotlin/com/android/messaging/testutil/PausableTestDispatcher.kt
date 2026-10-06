package com.android.messaging.testutil

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher

/**
 * Holds tasks dispatched while paused, so a test can start them in any order the way a
 * multi-threaded dispatcher may. Delays still run on [testDispatcher]'s scheduler.
 */
@OptIn(InternalCoroutinesApi::class)
internal class PausableTestDispatcher(
    private val testDispatcher: TestDispatcher,
) : CoroutineDispatcher(),
    Delay by testDispatcher {

    private val heldTasks = mutableListOf<HeldTask>()
    private var isPaused = false

    val heldTaskCount: Int
        get() = heldTasks.size

    fun pause() {
        isPaused = true
    }

    /** Hands the held task at [index], counted in dispatch order, to [testDispatcher]. */
    fun release(index: Int) {
        val (context, block) = heldTasks.removeAt(index)
        testDispatcher.dispatch(context = context, block = block)
    }

    fun resume() {
        isPaused = false
        while (heldTasks.isNotEmpty()) {
            release(index = 0)
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (isPaused) {
            heldTasks += HeldTask(context = context, block = block)
        } else {
            testDispatcher.dispatch(context = context, block = block)
        }
    }

    private data class HeldTask(
        val context: CoroutineContext,
        val block: Runnable,
    )
}
