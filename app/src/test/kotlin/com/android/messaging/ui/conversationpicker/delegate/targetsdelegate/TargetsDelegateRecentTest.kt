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
internal class TargetsDelegateRecentTest : BaseTargetsDelegateTest() {

    @Test
    fun initialState_beforeBind_isLoading() = runTest {
        val delegate = createDelegate()

        assertTrue(delegate.state.value.isLoading)
    }

    @Test
    fun bind_withRecentsOverLimit_showsFirstFiveAndAllowsLoadMore() = runTest {
        givenRecents(recents(count = 6))

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        val state = delegate.state.value
        assertFalse(state.isLoading)
        assertEquals(5, state.recent.targets.size)
        assertTrue(state.recent.canLoadMore)
        assertFalse(state.recent.canCollapse)
    }

    @Test
    fun bind_withRecentsUnderLimit_showsAllAndDisablesLoadMore() = runTest {
        givenRecents(recents(count = 3))

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        val state = delegate.state.value
        assertEquals(3, state.recent.targets.size)
        assertFalse(state.recent.canLoadMore)
        assertFalse(state.recent.canCollapse)
    }

    @Test
    fun loadMoreRecent_revealsMoreAndAllowsCollapse() = runTest {
        givenRecents(recents(count = 10))

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.loadMoreRecent()
        runCurrent()

        val state = delegate.state.value
        assertEquals(10, state.recent.targets.size)
        assertFalse(state.recent.canLoadMore)
        assertTrue(state.recent.canCollapse)
    }

    @Test
    fun loadMoreRecent_withManyRecents_keepsLoadMoreAndCollapse() = runTest {
        givenRecents(recents(count = 25))

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.loadMoreRecent()
        runCurrent()

        val state = delegate.state.value
        assertEquals(20, state.recent.targets.size)
        assertTrue(state.recent.canLoadMore)
        assertTrue(state.recent.canCollapse)
    }

    @Test
    fun collapseRecent_resetsToInitialLimit() = runTest {
        givenRecents(recents(count = 10))

        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.loadMoreRecent()
        runCurrent()

        delegate.collapseRecent()
        runCurrent()

        val state = delegate.state.value
        assertEquals(5, state.recent.targets.size)
        assertTrue(state.recent.canLoadMore)
        assertFalse(state.recent.canCollapse)
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
