package com.android.messaging.ui.search

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.data.search.repository.MessageSearchRepository
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<MessageSearchRepository>()

    @Test
    fun query_returnsMatchesAfterDebounce() = runTest(mainDispatcherRule.testDispatcher) {
        val expected = listOf(result("one"))
        every { repository.search("needle") } returns flowOf(expected.toImmutableList())

        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect { } }

        viewModel.onQueryChanged("needle")
        advanceTimeBy(201)
        advanceUntilIdle()

        assertEquals(expected, viewModel.results.value)
    }

    @Test
    fun blankQuery_doesNotQueryRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch { viewModel.results.collect { } }

        viewModel.onQueryChanged("   ")
        advanceTimeBy(201)
        advanceUntilIdle()

        assertEquals(emptyList<MessageSearchResult>(), viewModel.results.value)
        verify(exactly = 0) { repository.search(any()) }
    }

    @Test
    fun resultClicked_emitsConversationNavigationEvent() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = SearchViewModel(repository)
            viewModel.navigationEvents.test {
                viewModel.onResultClicked(ConversationId("c1"))

                assertEquals(ConversationId("c1"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun result(id: String): MessageSearchResult {
        return MessageSearchResult(
            messageId = MessageId("m$id"),
            conversationId = ConversationId("c$id"),
            snippet = "snippet $id",
            timestamp = 1_700_000_000_000L,
            conversationName = "Conversation $id",
            senderName = "Sender $id",
        )
    }
}
