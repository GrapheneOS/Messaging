package com.android.messaging.ui.conversationlist.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversationlist.matcher.ConversationSearchMatcherImpl
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.SearchableConversation
import com.android.messaging.data.conversationlist.model.SearchableMessage
import com.android.messaging.data.conversationlist.repository.ConversationListSearchRepository
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.mapper.ConversationListItemUiMapper
import com.android.messaging.ui.conversationlist.mapper.ConversationListSearchUiStateMapperImpl
import com.android.messaging.ui.conversationlist.model.ConversationListSearchContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListSearchUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationListSearchDelegateImplTest {

    private val conversations = MutableStateFlow<ImmutableList<SearchableConversation>>(
        persistentListOf(
            searchable(conversationId = ANNA_ID, title = "Anna"),
            searchable(conversationId = BOB_ID, title = "Bob"),
        ),
    )
    private val messages = MutableStateFlow<ImmutableList<SearchableMessage>>(persistentListOf())
    private val repository = mockk<ConversationListSearchRepository> {
        every { observeSearchableConversations() } returns conversations
        every { observeMessages(any()) } returns messages
    }
    private val itemUiMapper = mockk<ConversationListItemUiMapper> {
        every { map(any(), any(), any()) } answers {
            val item = firstArg<ConversationListItem>()

            previewConversationListItem(
                conversationId = item.conversationId,
                title = item.title.orEmpty(),
                snippetText = null,
            )
        }
    }
    private val savedStateHandle = SavedStateHandle()

    @Test
    fun bind_searchClosed_staysInactiveWithoutReadingConversations() {
        runTest {
            val delegate = createDelegate()

            delegate.bind(scope = backgroundScope)
            runCurrent()

            assertEquals(ConversationListSearchUiState(), delegate.state.value)
            verify(exactly = 0) { repository.observeSearchableConversations() }
        }
    }

    @Test
    fun open_turnsSearchOnBeforeAnyResultArrives() {
        runTest {
            val delegate = createDelegate()
            delegate.bind(scope = backgroundScope)

            delegate.open()

            assertEquals(ConversationListSearchUiState(isActive = true), delegate.state.value)
            assertEquals(true, savedStateHandle.get<Boolean>(SEARCH_ACTIVE_KEY))
        }
    }

    @Test
    fun setQuery_findsMatchesOnlyOnceTypingPauses() {
        runTest {
            val delegate = createBoundOpenDelegate()

            delegate.setQuery(query = "ann")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS - 1)
            runCurrent()

            assertEquals(ConversationListSearchContentUiState.Idle, delegate.state.value.content)

            advanceTimeBy(delayTimeMillis = 1)
            runCurrent()

            assertEquals(listOf(ANNA_ID), resultIds(delegate))
            assertEquals("ann", savedStateHandle.get<String>(SEARCH_QUERY_KEY))
        }
    }

    @Test
    fun setQuery_cleared_returnsToIdleWithoutWaiting() {
        runTest {
            val delegate = createBoundOpenDelegate()
            delegate.setQuery(query = "ann")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()

            delegate.setQuery(query = "")
            runCurrent()

            assertEquals(ConversationListSearchContentUiState.Idle, delegate.state.value.content)
            verify(exactly = 0) { repository.observeMessages(query = "") }
        }
    }

    @Test
    fun setQuery_findsMessagesForTheQueryBelowTheConversations() {
        runTest {
            val delegate = createBoundOpenDelegate()
            messages.value = persistentListOf(message(conversationId = BOB_ID, text = "Ann's car"))

            delegate.setQuery(query = "ann")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()

            assertEquals(listOf(ANNA_ID), resultIds(delegate))
            assertEquals(listOf(MESSAGE_ID), resultMessageIds(delegate))
            verify(exactly = 1) { repository.observeMessages(query = "ann") }
        }
    }

    @Test
    fun messagesChange_whileSearching_updatesResults() {
        runTest {
            val delegate = createBoundOpenDelegate()
            delegate.setQuery(query = "car")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()

            assertEquals(
                ConversationListSearchContentUiState.NoResults,
                delegate.state.value.content,
            )

            messages.value = persistentListOf(message(conversationId = BOB_ID, text = "New car"))
            runCurrent()

            assertEquals(emptyList<ConversationId>(), resultIds(delegate))
            assertEquals(listOf(MESSAGE_ID), resultMessageIds(delegate))
        }
    }

    @Test
    fun setQuery_nothingMatches_hasNoResults() {
        runTest {
            val delegate = createBoundOpenDelegate()

            delegate.setQuery(query = "zed")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()

            assertEquals(
                ConversationListSearchContentUiState.NoResults,
                delegate.state.value.content,
            )
        }
    }

    @Test
    fun conversationsChange_whileSearching_updatesResults() {
        runTest {
            val delegate = createBoundOpenDelegate()
            delegate.setQuery(query = "b")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()

            conversations.value = persistentListOf(
                searchable(conversationId = BOB_ID, title = "Bob"),
                searchable(conversationId = BEA_ID, title = "Bea"),
            )
            runCurrent()

            assertEquals(listOf(BOB_ID, BEA_ID), resultIds(delegate))
        }
    }

    @Test
    fun close_turnsSearchOffAndForgetsTheQuery() {
        runTest {
            val delegate = createBoundOpenDelegate()
            delegate.setQuery(query = "ann")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()

            delegate.close()
            runCurrent()

            assertEquals(ConversationListSearchUiState(), delegate.state.value)
            assertEquals(false, savedStateHandle.get<Boolean>(SEARCH_ACTIVE_KEY))
            assertEquals("", savedStateHandle.get<String>(SEARCH_QUERY_KEY))
        }
    }

    @Test
    fun openAndClose_repeated_areIgnored() {
        runTest {
            val delegate = createDelegate()
            delegate.bind(scope = backgroundScope)

            delegate.close()
            assertFalse(savedStateHandle.contains(SEARCH_ACTIVE_KEY))

            delegate.open()
            delegate.setQuery(query = "ann")
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()
            delegate.open()

            assertEquals(listOf(ANNA_ID), resultIds(delegate))
        }
    }

    @Test
    fun bind_twice_observesConversationsOnce() {
        runTest {
            val delegate = createDelegate()
            delegate.open()

            delegate.bind(scope = backgroundScope)
            delegate.bind(scope = backgroundScope)
            runCurrent()

            verify(exactly = 1) { repository.observeSearchableConversations() }
        }
    }

    @Test
    fun restoredSearch_resumesWithTheSavedQuery() {
        runTest {
            savedStateHandle[SEARCH_ACTIVE_KEY] = true
            savedStateHandle[SEARCH_QUERY_KEY] = "bob"
            val delegate = createDelegate()

            assertTrue(delegate.state.value.isActive)

            delegate.bind(scope = backgroundScope)
            advanceTimeBy(delayTimeMillis = DEBOUNCE_MILLIS)
            runCurrent()

            assertEquals(listOf(BOB_ID), resultIds(delegate))
        }
    }

    private fun TestScope.createBoundOpenDelegate(): ConversationListSearchDelegateImpl {
        val delegate = createDelegate()
        delegate.bind(scope = backgroundScope)
        delegate.open()
        runCurrent()

        return delegate
    }

    private fun TestScope.createDelegate(): ConversationListSearchDelegateImpl {
        return ConversationListSearchDelegateImpl(
            repository = repository,
            matcher = ConversationSearchMatcherImpl(),
            uiStateMapper = ConversationListSearchUiStateMapperImpl(itemUiMapper = itemUiMapper),
            savedStateHandle = savedStateHandle,
            defaultDispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    private fun resultIds(delegate: ConversationListSearchDelegate): List<ConversationId> {
        val content = delegate.state.value.content as ConversationListSearchContentUiState.Results

        return content.conversations.map { item -> item.conversationId }
    }

    private fun resultMessageIds(delegate: ConversationListSearchDelegate): List<MessageId> {
        val content = delegate.state.value.content as ConversationListSearchContentUiState.Results

        return content.messages.map { message -> message.messageId }
    }

    private fun message(
        conversationId: ConversationId,
        text: String,
    ): SearchableMessage {
        return SearchableMessage(
            messageId = MESSAGE_ID,
            conversationId = conversationId,
            text = text,
            receivedTimestamp = 0L,
            isIncoming = true,
            senderName = null,
        )
    }

    private fun searchable(
        conversationId: ConversationId,
        title: String,
    ): SearchableConversation {
        return SearchableConversation(
            item = conversationItem(conversationId = conversationId).copy(title = title),
            participants = persistentListOf(),
        )
    }

    private companion object {
        private const val SEARCH_ACTIVE_KEY = "conversation_list_search_active"
        private const val SEARCH_QUERY_KEY = "conversation_list_search_query"
        private const val DEBOUNCE_MILLIS = 150L

        private val ANNA_ID = ConversationId("1")
        private val BOB_ID = ConversationId("2")
        private val BEA_ID = ConversationId("3")
        private val MESSAGE_ID = MessageId("10")
    }
}
