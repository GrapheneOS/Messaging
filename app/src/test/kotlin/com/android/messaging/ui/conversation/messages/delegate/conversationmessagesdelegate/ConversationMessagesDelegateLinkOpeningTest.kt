package com.android.messaging.ui.conversation.messages.delegate.conversationmessagesdelegate

import com.android.messaging.data.appsettings.model.LinkOpeningMode
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.messages.delegate.ConversationMessagesDelegateImpl
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState.Present
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
internal class ConversationMessagesDelegateLinkOpeningTest :
    BaseConversationMessagesDelegateTest() {

    @Test
    fun refresh_followsTheLinkOpeningMode() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val messageData = mockk<ConversationMessageData>()
            val message = messageUiModel(messageId = "message-1")
            coEvery {
                appSettingsRepository.getLinkOpeningMode()
            } returnsMany listOf(
                LinkOpeningMode.AskBeforeOpening,
                LinkOpeningMode.DoNotOpen,
                LinkOpeningMode.OpenOnTap,
            )
            every { messageUiModelMapper.map(data = messageData) } returns message
            givenConversationMessages(messages = flowOf(listOf(messageData)))

            val delegate = createBoundDelegate(
                conversationIdFlow = MutableStateFlow(CONVERSATION_ID),
            )
            runCurrent()
            val modeAfterEachRefresh = buildList {
                add(delegate.presentState().linkOpeningMode)
                repeat(times = 2) {
                    delegate.refresh()
                    runCurrent()
                    add(delegate.presentState().linkOpeningMode)
                }
            }

            assertEquals(
                listOf(
                    LinkOpeningMode.AskBeforeOpening,
                    LinkOpeningMode.DoNotOpen,
                    LinkOpeningMode.OpenOnTap,
                ),
                modeAfterEachRefresh,
            )
            assertEquals(
                persistentListOf(message),
                delegate.presentState().messages,
            )
            verify(exactly = 1) { messageUiModelMapper.map(data = messageData) }
        }
    }

    private fun ConversationMessagesDelegateImpl.presentState(): Present {
        return state.value as Present
    }
}
