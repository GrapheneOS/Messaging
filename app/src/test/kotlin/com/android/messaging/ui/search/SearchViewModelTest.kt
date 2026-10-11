package com.android.messaging.ui.search

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.data.search.repository.MessageSearchRepository
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.entry.ConversationLaunchStore
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.search.SearchViewModel.UiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
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
    private val launchStore = mockk<ConversationLaunchStore>(relaxed = true)

    @Test
    fun query_returnsMatchesAfterDebounce() = runTest(mainDispatcherRule.testDispatcher) {
        val expected = listOf(result("one"))
        every { repository.search("needle") } returns flowOf(expected.toImmutableList())

        val viewModel = SearchViewModel(repository, launchStore)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.onQueryChanged("needle")
        advanceTimeBy(201)
        advanceUntilIdle()

        assertEquals(UiState.Results(expected.toImmutableList()), viewModel.uiState.value)
    }

    @Test
    fun query_isSearchingBeforeResultsArrive() = runTest(mainDispatcherRule.testDispatcher) {
        val expected = listOf(result("one"))
        every { repository.search("needle") } returns flow {
            delay(100)
            emit(expected.toImmutableList())
        }

        val viewModel = SearchViewModel(repository, launchStore)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.onQueryChanged("needle")
        advanceTimeBy(201)

        assertEquals(UiState.Searching, viewModel.uiState.value)

        advanceTimeBy(100)
        advanceUntilIdle()

        assertEquals(UiState.Results(expected.toImmutableList()), viewModel.uiState.value)
    }

    @Test
    fun blankQuery_idlesWithoutQueryingRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = SearchViewModel(repository, launchStore)
        backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.onQueryChanged("   ")
        advanceTimeBy(201)
        advanceUntilIdle()

        assertEquals(UiState.Idle, viewModel.uiState.value)
        verify(exactly = 0) { repository.search(any()) }
    }

    @Test
    fun resultClicked_opensConversationScrolledToTheMatchingMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = SearchViewModel(repository, launchStore)
            viewModel.navigationEvents.test {
                viewModel.onResultClicked(result("1"))

                assertEquals(ConversationId("c1"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                launchStore.submit(
                    request = ConversationEntryLaunchRequest(
                        conversationId = ConversationId("c1"),
                        messageId = MessageId("m1"),
                    ),
                )
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
