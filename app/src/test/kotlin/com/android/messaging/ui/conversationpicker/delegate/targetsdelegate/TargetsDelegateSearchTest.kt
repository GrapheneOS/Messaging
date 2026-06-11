package com.android.messaging.ui.conversationpicker.delegate.targetsdelegate

import com.android.messaging.data.conversationpicker.model.TargetConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TargetsDelegateSearchTest : BaseTargetsDelegateTest() {

    @Test
    fun setSearchActive_isReflectedInState() = runTest {
        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.setSearchActive(true)
        runCurrent()

        assertTrue(delegate.state.value.isSearchActive)
    }

    @Test
    fun setSearchQuery_filtersRecentsByDisplayName() = runTest {
        givenRecents(
            listOf(
                shareTargetConversation(
                    conversationId = "1",
                    name = "Alice",
                ),
                shareTargetConversation(
                    conversationId = "2",
                    name = "Bob",
                ),
            ),
        )

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.setSearchActive(true)
        delegate.setSearchQuery("ali")
        runCurrent()

        val recents = delegate.state.value.recent.targets
        assertEquals(listOf("Alice"), recents.map { it.displayName })
    }

    @Test
    fun setSearchQuery_filtersRecentsByDetails() = runTest {
        givenRecents(
            listOf(
                shareTargetConversation(
                    conversationId = "1",
                    name = "Alice",
                    normalizedDestination = "+15551234",
                ),
                shareTargetConversation(
                    conversationId = "2",
                    name = "Bob",
                    normalizedDestination = "+15559999",
                ),
            ),
        )

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.setSearchActive(true)
        delegate.setSearchQuery("1234")
        runCurrent()

        val recents = delegate.state.value.recent.targets
        assertEquals(listOf("Alice"), recents.map { it.displayName })
    }

    @Test
    fun setSearchQuery_hidesLoadMoreAndCollapseAffordances() = runTest {
        givenRecents(recents(count = 9))

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.setSearchActive(true)
        delegate.setSearchQuery("Conversation")
        runCurrent()

        val state = delegate.state.value
        assertFalse(state.recent.canLoadMore)
        assertFalse(state.recent.canCollapse)
    }

    @Test
    fun setSearchActiveFalse_clearsQueryAndRestoresLimitedRecents() = runTest {
        givenRecents(recents(count = 12))

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.setSearchActive(true)
        delegate.setSearchQuery("Conversation 12")
        runCurrent()

        delegate.setSearchActive(false)
        runCurrent()

        val state = delegate.state.value
        assertFalse(state.isSearchActive)
        assertEquals(5, state.recent.targets.size)
        assertTrue(state.recent.canLoadMore)
    }

    private fun recents(count: Int): List<TargetConversation> {
        return (1..count).map { index ->
            shareTargetConversation(
                conversationId = index.toString(),
                name = "Conversation $index",
            )
        }
    }
}
