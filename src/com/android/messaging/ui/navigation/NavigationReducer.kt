package com.android.messaging.ui.navigation

import androidx.navigation3.runtime.NavKey

internal interface NavigationReducer {

    fun push(
        backStack: MutableList<NavKey>,
        destination: NavKey,
    )

    fun pop(
        backStack: MutableList<NavKey>,
    ): Boolean

    fun replaceTop(
        backStack: MutableList<NavKey>,
        destination: NavKey,
    )

    fun reset(
        backStack: MutableList<NavKey>,
        destinations: List<NavKey>,
    )
}

internal class NavigationReducerImpl : NavigationReducer {

    override fun push(
        backStack: MutableList<NavKey>,
        destination: NavKey,
    ) {
        destination
            .takeIf { it != backStack.lastOrNull() }
            ?.let(backStack::add)
    }

    override fun pop(
        backStack: MutableList<NavKey>,
    ): Boolean {
        if (backStack.size <= 1) {
            return false
        }

        backStack.removeAt(backStack.lastIndex)

        return true
    }

    override fun replaceTop(
        backStack: MutableList<NavKey>,
        destination: NavKey,
    ) {
        if (backStack.isEmpty()) {
            backStack.add(destination)
            return
        }

        backStack[backStack.lastIndex] = destination
    }

    override fun reset(
        backStack: MutableList<NavKey>,
        destinations: List<NavKey>,
    ) {
        if (destinations.isEmpty() || backStack == destinations) {
            return
        }

        backStack.clear()
        backStack.addAll(destinations)
    }
}
