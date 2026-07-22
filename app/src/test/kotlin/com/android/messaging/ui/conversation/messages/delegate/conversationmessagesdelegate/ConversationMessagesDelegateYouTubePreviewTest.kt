package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
internal class ConversationMessagesDelegateYouTubePreviewTest :
    BaseConversationMessagesDelegateTest() {

    @Test
    fun refresh_forwardsUpdatedYouTubeLinkPreviewsPreferenceToMapper() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageData = mockk<ConversationMessageData>()
            val disabledMessage = messageUiModel(messageId = "disabled")
            val enabledMessage = messageUiModel(messageId = "enabled")
            coEvery {
                appSettingsRepository.isYouTubeLinkPreviewsEnabled()
            } returnsMany listOf(false, true)
            every {
                messageUiModelMapper.map(
                    data = messageData,
                    isYouTubePreviewEnabled = false,
                )
            } returns disabledMessage
            every {
                messageUiModelMapper.map(
                    data = messageData,
                    isYouTubePreviewEnabled = true,
                )
            } returns enabledMessage
            givenConversationMessages(messages = flowOf(listOf(messageData)))
            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(disabledMessage)),
                delegate.state.value,
            )

            delegate.refresh()
            runCurrent()

            assertEquals(
                ConversationMessagesUiState.Present(persistentListOf(enabledMessage)),
                delegate.state.value,
            )
            verify(exactly = 1) {
                messageUiModelMapper.map(
                    data = messageData,
                    isYouTubePreviewEnabled = false,
                )
            }
            verify(exactly = 1) {
                messageUiModelMapper.map(
                    data = messageData,
                    isYouTubePreviewEnabled = true,
                )
            }
        }
    }
}
