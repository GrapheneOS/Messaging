package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessagesDelegateYouTubePreviewTest :
    BaseConversationMessagesDelegateTest() {

    @Test
    fun refresh_exposesUpdatedYouTubeLinkPreviewsPreferenceWithoutRemappingMessages() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageData = mockk<ConversationMessageData>()
            val message = messageUiModel(messageId = "message-1")
            coEvery {
                appSettingsRepository.isYouTubeLinkPreviewsEnabled()
            } returnsMany listOf(false, true)
            every { messageUiModelMapper.map(data = messageData) } returns message
            givenConversationMessages(messages = flowOf(listOf(messageData)))

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    messages = persistentListOf(message),
                    youTubeLinkPreviewsEnabled = false,
                ),
                delegate.state.value,
            )

            delegate.refresh()
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(
                    messages = persistentListOf(message),
                    youTubeLinkPreviewsEnabled = true,
                ),
                delegate.state.value,
            )
            verify(exactly = 1) { messageUiModelMapper.map(data = messageData) }
        }
    }

    @Test
    fun refresh_withChangedPreference_keepsResolvedVCardMetadata() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val contentUri = "content://vcard/1"
            val loadingUiModel = vCardUiModel(titleText = "loading")
            val loadedUiModel = vCardUiModel(titleText = "Alice")

            givenVCardMetadata(
                contentUri = contentUri,
                metadata = flow {
                    emit(ConversationVCardAttachmentMetadata.Loading)
                    emit(ConversationVCardAttachmentMetadata.Missing)
                },
            )
            givenVCardUiModel(ConversationVCardAttachmentMetadata.Loading, loadingUiModel)
            givenVCardUiModel(ConversationVCardAttachmentMetadata.Missing, loadedUiModel)

            val messageData = mockk<ConversationMessageData>()
            every { messageUiModelMapper.map(data = messageData) } returns messageUiModel(
                messageId = "message-1",
                parts = listOf(vCardPart(contentUri = contentUri)),
            )
            coEvery {
                appSettingsRepository.isYouTubeLinkPreviewsEnabled()
            } returnsMany listOf(false, true)
            givenConversationMessages(messages = flowOf(listOf(messageData)))

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            val vCardTitles = mutableListOf<String?>()
            backgroundScope.launch {
                delegate.state.collect { state ->
                    if (state is ConversationMessagesUiState.Present) {
                        vCardTitles += state.messages
                            .flatMap { it.parts }
                            .filterIsInstance<ConversationMessagePartUiModel.Attachment.VCard>()
                            .map { it.vCardUiModel?.titleText }
                    }
                }
            }
            runCurrent()

            delegate.refresh()
            runCurrent()

            assertEquals(1, vCardTitles.count { it == "loading" })
            assertEquals("Alice", vCardTitles.last())
            assertEquals(
                true,
                (delegate.state.value as ConversationMessagesUiState.Present)
                    .youTubeLinkPreviewsEnabled,
            )
        }
    }
}
