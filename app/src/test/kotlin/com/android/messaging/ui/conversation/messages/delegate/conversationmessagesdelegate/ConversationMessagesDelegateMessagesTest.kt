package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
internal class ConversationMessagesDelegateMessagesTest : BaseConversationMessagesDelegateTest() {

    @Test
    fun bind_withEmptyMessages_emitsPresentWithNoMessages() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenConversationMessages(messages = flowOf(emptyList()))
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf()),
                delegate.state.value,
            )
        }
    }

    @Test
    fun bind_withMessagesWithoutVCardParts_emitsPresentWithMappedMessages() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = messageUiModel(
                messageId = "first",
                parts = listOf(textPart(text = "hi"), imagePart()),
            )
            val second = messageUiModel(messageId = "second", parts = listOf(textPart()))
            givenConversationMessages(messages = flowOf(messagesOf(first, second)))
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(first, second)),
                delegate.state.value,
            )
            verify(exactly = 0) {
                @Suppress("UnusedFlow")
                vCardMetadataRepository.observeAttachmentMetadata(contentUri = any())
            }
        }
    }

    @Test
    fun bind_mapsEveryMessageDataThroughMessageMapper() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val first = messageUiModel(messageId = "first")
            val second = messageUiModel(messageId = "second")
            val messageData = messagesOf(first, second)
            givenConversationMessages(messages = flowOf(messageData))
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(first, second)),
                delegate.state.value,
            )
            verify(exactly = 1) { messageUiModelMapper.map(data = messageData[0]) }
            verify(exactly = 1) { messageUiModelMapper.map(data = messageData[1]) }
        }
    }

    @Test
    fun bind_whenRepositoryEmitsNewMessageList_updatesPresentState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val initial = messageUiModel(messageId = "initial")
            val updated = messageUiModel(messageId = "updated")
            val messagesFlow = MutableStateFlow(messagesOf(initial))
            givenConversationMessages(messages = messagesFlow)
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(initial)),
                delegate.state.value,
            )

            messagesFlow.value = messagesOf(updated)
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(updated)),
                delegate.state.value,
            )
        }
    }
}
