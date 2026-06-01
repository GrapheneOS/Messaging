package com.android.messaging.testutil

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

internal class TestLifecycleOwner(
    initialState: Lifecycle.State,
) : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        lifecycleRegistry.currentState = initialState
    }

    override val lifecycle: Lifecycle
        get() {
            return lifecycleRegistry
        }

    fun moveTo(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }
}
