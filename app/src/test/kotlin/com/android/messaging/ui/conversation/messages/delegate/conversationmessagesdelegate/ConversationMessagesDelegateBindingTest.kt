package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import app.cash.turbine.test
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesDelegateBindingTest : BaseConversationMessagesDelegateTest() {

    @Test
    fun bind_fromLoadingToPresent_emitsBothStates() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val message = messageUiModel(messageId = "message-1")
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val delegate = createDelegate()

            delegate.state.test {
                assertEquals(ConversationMessagesUiState.Loading, awaitItem())

                delegate.bind(
                    scope = backgroundScope,
                    conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
                )
                runCurrent()

                assertEquals(
                    ConversationMessagesUiState.Present(persistentListOf(message)),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun bind_withNullConversationId_staysLoadingWithoutQueryingRepository() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createBoundDelegate(conversationIdFlow = MutableStateFlow(null))
            runCurrent()

            assertEquals(ConversationMessagesUiState.Loading, delegate.state.value)
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                conversationsRepository.getConversationMessages(conversationId = any())
            }
        }
    }

    @Test
    fun bind_calledTwice_ignoresSecondBinding() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val message = messageUiModel(messageId = "message-1")
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val reboundMessage = messageUiModel(messageId = "rebound")
            givenConversationMessages(
                messages = flowOf(messagesOf(reboundMessage)),
                conversationId = "conversation-rebound",
            )

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            delegate.bind(
                scope = backgroundScope,
                conversationIdFlow = MutableStateFlow("conversation-rebound"),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(message)),
                delegate.state.value,
            )
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                conversationsRepository.getConversationMessages(
                    conversationId = "conversation-rebound",
                )
            }
        }
    }

    @Test
    fun bind_whenConversationIdBecomesNull_resetsToLoading() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val message = messageUiModel(messageId = "message-1")
            givenConversationMessages(messages = flowOf(messagesOf(message)))
            val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()

            conversationIdFlow.value = null
            runCurrent()

            assertEquals(ConversationMessagesUiState.Loading, delegate.state.value)
        }
    }

    @Test
    fun bind_whenConversationIdChanges_reobservesNewConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val firstMessage = messageUiModel(messageId = "first")
            val secondMessage = messageUiModel(messageId = "second")
            givenConversationMessages(messages = flowOf(messagesOf(firstMessage)))
            givenConversationMessages(
                messages = flowOf(messagesOf(secondMessage)),
                conversationId = "conversation-2",
            )
            val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()

            conversationIdFlow.value = "conversation-2"
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(secondMessage)),
                delegate.state.value,
            )
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationsRepository.getConversationMessages(conversationId = CONVERSATION_ID)
            }
            verify(exactly = 1) {
                @Suppress("UnusedFlow")
                conversationsRepository.getConversationMessages(conversationId = "conversation-2")
            }
        }
    }

    @Test
    fun bind_whenConversationIdChangesToUnloadedConversation_resetsToLoading() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val firstMessage = messageUiModel(messageId = "first")
            givenConversationMessages(messages = flowOf(messagesOf(firstMessage)))
            val pendingSecondMessages = MutableSharedFlow<List<ConversationMessageData>>(replay = 1)
            givenConversationMessages(
                messages = pendingSecondMessages,
                conversationId = "conversation-2",
            )
            val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()
            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(firstMessage)),
                delegate.state.value,
            )

            conversationIdFlow.value = "conversation-2"
            runCurrent()
            assertEquals(ConversationMessagesUiState.Loading, delegate.state.value)

            val secondMessage = messageUiModel(messageId = "second")
            pendingSecondMessages.tryEmit(messagesOf(secondMessage))
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(secondMessage)),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_whenConversationIdChangesBeforeFirstEmission_ignoresStaleEmission() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val pendingFirstMessages = MutableSharedFlow<List<ConversationMessageData>>(replay = 1)
            givenConversationMessages(messages = pendingFirstMessages)
            val secondMessage = messageUiModel(messageId = "second")
            givenConversationMessages(
                messages = flowOf(messagesOf(secondMessage)),
                conversationId = "conversation-2",
            )
            val conversationIdFlow = MutableStateFlow<String?>(CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            runCurrent()
            assertEquals(ConversationMessagesUiState.Loading, delegate.state.value)

            conversationIdFlow.value = "conversation-2"
            runCurrent()
            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(secondMessage)),
                delegate.state.value,
            )

            pendingFirstMessages.tryEmit(messagesOf(messageUiModel(messageId = "stale-first")))
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(secondMessage)),
                delegate.state.value,
            )
        }
    }
}
