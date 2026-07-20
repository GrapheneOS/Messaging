package com.android.messaging.ui.navigation

import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NavigationReducerImplTest {

    private val reducer: NavigationReducer = NavigationReducerImpl()

    @Test
    fun reset_leavesNavBackStackUnmodifiedWhenDestinationsMatch() {
        val backStack = NavBackStack(ConversationListNavKey, OnboardingNavKey)

        val modified = recordModifications {
            reducer.reset(
                backStack = backStack,
                destinations = listOf(ConversationListNavKey, OnboardingNavKey),
            )
        }

        assertFalse(modified)
    }

    @Test
    fun reset_replacesNavBackStackEntriesWhenDestinationsDiffer() {
        val backStack = NavBackStack(ConversationListNavKey, OnboardingNavKey)

        val modified = recordModifications {
            reducer.reset(
                backStack = backStack,
                destinations = listOf(ConversationListNavKey),
            )
        }

        assertTrue(modified)
        assertEquals(listOf(ConversationListNavKey), backStack.toList())
    }

    private fun recordModifications(block: () -> Unit): Boolean {
        val snapshot = Snapshot.takeMutableSnapshot()

        try {
            snapshot.enter(block)

            val modified = snapshot.hasPendingChanges()
            snapshot.apply().check()

            return modified
        } finally {
            snapshot.dispose()
        }
    }
}
