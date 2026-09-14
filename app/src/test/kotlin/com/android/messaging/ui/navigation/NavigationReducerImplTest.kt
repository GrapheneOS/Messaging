package com.android.messaging.ui.navigation

import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.navigation.ConversationNavKey
import com.android.messaging.ui.conversation.navigation.NewChatNavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.conversationsettings.navigation.ConversationSettingsNavKey
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
    fun push_appendsDestinationToBackStack() {
        val backStack = mutableListOf<NavKey>(ConversationListNavKey)

        reducer.push(
            backStack = backStack,
            destination = ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        assertEquals(
            listOf(
                ConversationListNavKey,
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun push_appendsDestinationToEmptyBackStack() {
        val backStack = mutableListOf<NavKey>()

        reducer.push(
            backStack = backStack,
            destination = ConversationListNavKey,
        )

        assertEquals(listOf(ConversationListNavKey), backStack)
    }

    @Test
    fun push_ignoresDestinationEqualToTop() {
        val backStack = mutableListOf(
            ConversationListNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        reducer.push(
            backStack = backStack,
            destination = ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        assertEquals(
            listOf(
                ConversationListNavKey,
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun push_appendsDestinationThatIsPresentButNotOnTop() {
        val backStack = mutableListOf<NavKey>(
            ConversationNavKey(conversationId = CONVERSATION_ID),
            ConversationSettingsNavKey(conversationId = CONVERSATION_ID),
        )

        reducer.push(
            backStack = backStack,
            destination = ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        assertEquals(
            listOf(
                ConversationNavKey(conversationId = CONVERSATION_ID),
                ConversationSettingsNavKey(conversationId = CONVERSATION_ID),
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun pop_removesTopAndReportsHandledWhenBackStackHasMultipleEntries() {
        val backStack = mutableListOf(
            ConversationListNavKey,
            ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        val handled = reducer.pop(backStack = backStack)

        assertTrue(handled)
        assertEquals(listOf(ConversationListNavKey), backStack)
    }

    @Test
    fun pop_reportsUnhandledAtRootAndKeepsRootDestination() {
        val backStack = mutableListOf<NavKey>(ConversationListNavKey)

        val handled = reducer.pop(backStack = backStack)

        assertFalse(handled)
        assertEquals(listOf(ConversationListNavKey), backStack)
    }

    @Test
    fun pop_reportsUnhandledWhenBackStackIsEmpty() {
        val backStack = mutableListOf<NavKey>()

        val handled = reducer.pop(backStack = backStack)

        assertFalse(handled)
        assertEquals(emptyList<NavKey>(), backStack)
    }

    @Test
    fun replaceTop_swapsTopDestinationAndKeepsTheRest() {
        val backStack = mutableListOf(
            ConversationListNavKey,
            NewChatNavKey,
        )

        reducer.replaceTop(
            backStack = backStack,
            destination = ConversationNavKey(conversationId = CONVERSATION_ID),
        )

        assertEquals(
            listOf(
                ConversationListNavKey,
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun replaceTop_addsDestinationWhenBackStackIsEmpty() {
        val backStack = mutableListOf<NavKey>()

        reducer.replaceTop(
            backStack = backStack,
            destination = ConversationListNavKey,
        )

        assertEquals(listOf(ConversationListNavKey), backStack)
    }

    @Test
    fun reset_replacesEveryDestination() {
        val backStack = mutableListOf(
            ConversationListNavKey,
            NewChatNavKey,
        )

        reducer.reset(
            backStack = backStack,
            destinations = listOf(
                ConversationListNavKey,
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
        )

        assertEquals(
            listOf(
                ConversationListNavKey,
                ConversationNavKey(conversationId = CONVERSATION_ID),
            ),
            backStack,
        )
    }

    @Test
    fun reset_ignoresEmptyDestinations() {
        val backStack = RecordingBackStack(listOf(ConversationListNavKey, NewChatNavKey))

        reducer.reset(
            backStack = backStack,
            destinations = emptyList(),
        )

        assertEquals(0, backStack.clearCount)
        assertEquals(listOf(ConversationListNavKey, NewChatNavKey), backStack)
    }

    @Test
    fun reset_doesNotTouchBackStackWhenDestinationsAlreadyMatch() {
        val backStack = RecordingBackStack(listOf(ConversationListNavKey, NewChatNavKey))

        reducer.reset(
            backStack = backStack,
            destinations = listOf(ConversationListNavKey, NewChatNavKey),
        )

        assertEquals(0, backStack.clearCount)
        assertEquals(listOf(ConversationListNavKey, NewChatNavKey), backStack)
    }

    @Test
    fun reset_leavesNavBackStackUnmodifiedWhenDestinationsMatch() {
        val backStack = NavBackStack(ConversationListNavKey, NewChatNavKey)

        val modified = recordModifications {
            reducer.reset(
                backStack = backStack,
                destinations = listOf(ConversationListNavKey, NewChatNavKey),
            )
        }

        assertFalse(modified)
    }

    @Test
    fun reset_replacesNavBackStackEntriesWhenDestinationsDiffer() {
        val backStack = NavBackStack(ConversationListNavKey, NewChatNavKey)

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

    private class RecordingBackStack(
        destinations: List<NavKey>,
    ) : ArrayList<NavKey>(destinations) {

        var clearCount = 0
            private set

        override fun clear() {
            clearCount++
            super.clear()
        }
    }
}
