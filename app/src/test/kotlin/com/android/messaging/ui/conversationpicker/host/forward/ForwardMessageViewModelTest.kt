package com.android.messaging.ui.conversationpicker.host.forward

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.conversation.usecase.forward.CreateForwardedMessage
import com.android.messaging.domain.conversationpicker.model.SendContentResult
import com.android.messaging.domain.conversationpicker.model.SendTarget
import com.android.messaging.domain.conversationpicker.usecase.BuildConversationDraftFromMessage
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_CONVERSATION_ID_VALUE as CONVERSATION_ID_VALUE
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.conversation.navigation.ConversationDraftLauncher
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ForwardMessageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val conversationDraftLauncher = mockk<ConversationDraftLauncher>(relaxed = true)
    private val sendContentToTargets = mockk<SendContentToTargets>()
    private val buildConversationDraftFromMessage = mockk<BuildConversationDraftFromMessage>()
    private val createForwardedMessage = mockk<CreateForwardedMessage>()

    @Test
    fun uiState_afterInit_carriesTheMessageRebuiltFromTheNavigationArguments() = runTest {
        val forwardedMessage = MessageData()

        givenForwardedMessage(
            message = forwardedMessage,
            draft = FORWARDED_DRAFT,
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value

        assertSame(forwardedMessage, uiState.message)
        assertSame(FORWARDED_DRAFT, uiState.draft)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun uiState_whenTheMessageCannotBeRebuilt_settlesWithoutADraft() = runTest {
        givenNoForwardedMessage()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNull(uiState.message)
        assertNull(uiState.draft)
        assertFalse(uiState.isLoading)
        verify(exactly = 0) { buildConversationDraftFromMessage(any()) }
    }

    @Test
    fun onTargetSelected_launchesTheTargetConversationWithTheRebuiltMessage() = runTest {
        val forwardedMessage = MessageData()

        givenForwardedMessage(message = forwardedMessage, draft = FORWARDED_DRAFT)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onTargetSelected(conversationId = TARGET_CONVERSATION_ID)

        verify {
            conversationDraftLauncher.launch(
                conversationId = TARGET_CONVERSATION_ID,
                draft = forwardedMessage,
            )
        }
    }

    @Test
    fun onTargetSelected_emitsOpenConversationForTheTarget() = runTest {
        givenForwardedMessage(message = MessageData(), draft = FORWARDED_DRAFT)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.navigationEvents.test {
            viewModel.onTargetSelected(conversationId = TARGET_CONVERSATION_ID)

            assertThat(awaitItem())
                .isEqualTo(ForwardMessageNavEvent.OpenConversation(TARGET_CONVERSATION_ID))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSendToTargets_emitsCloseWithoutWaitingForTheSend() = runTest {
        givenForwardedMessage(message = MessageData(), draft = FORWARDED_DRAFT)
        coEvery { sendContentToTargets(FORWARDED_DRAFT, TARGETS) } coAnswers { awaitCancellation() }

        val viewModel = createViewModel(applicationScope = backgroundScope)
        advanceUntilIdle()

        viewModel.navigationEvents.test {
            viewModel.onSendToTargets(
                targets = TARGETS,
                draft = FORWARDED_DRAFT,
                onSendFailure = {},
            )

            assertThat(awaitItem()).isEqualTo(ForwardMessageNavEvent.Close)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onSendToTargets_whenSendSucceeds_doesNotReportAFailure() = runTest {
        givenForwardedMessage(message = MessageData(), draft = FORWARDED_DRAFT)
        coEvery { sendContentToTargets(FORWARDED_DRAFT, TARGETS) } returns SendContentResult.Success

        val viewModel = createViewModel(applicationScope = this)
        advanceUntilIdle()
        var didReportFailure = false

        viewModel.onSendToTargets(
            targets = TARGETS,
            draft = FORWARDED_DRAFT,
            onSendFailure = { didReportFailure = true },
        )
        advanceUntilIdle()

        assertFalse(didReportFailure)
    }

    @Test
    fun onSendToTargets_whenSendFails_reportsTheFailure() = runTest {
        givenForwardedMessage(message = MessageData(), draft = FORWARDED_DRAFT)
        coEvery { sendContentToTargets(FORWARDED_DRAFT, TARGETS) } returns SendContentResult.Failure

        val viewModel = createViewModel(applicationScope = this)
        advanceUntilIdle()
        var didReportFailure = false

        viewModel.onSendToTargets(
            targets = TARGETS,
            draft = FORWARDED_DRAFT,
            onSendFailure = { didReportFailure = true },
        )
        advanceUntilIdle()

        assertTrue(didReportFailure)
    }

    @Test
    fun init_withoutConversationIdArgument_reportsTheMissingArgument() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            createViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(FORWARD_MESSAGE_ID_ARG to MESSAGE_ID_VALUE),
                ),
            )
        }

        assertThat(error.message).isEqualTo("conversationId is required")
    }

    @Test
    fun init_withoutMessageIdArgument_reportsTheMissingArgument() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            createViewModel(
                savedStateHandle = SavedStateHandle(
                    mapOf(FORWARD_CONVERSATION_ID_ARG to CONVERSATION_ID_VALUE),
                ),
            )
        }

        assertThat(error.message).isEqualTo("messageId is required")
    }

    private fun givenForwardedMessage(
        message: MessageData,
        draft: ConversationDraft,
    ) {
        givenRebuiltMessage(message = message)
        every { buildConversationDraftFromMessage(message) } returns draft
    }

    private fun givenNoForwardedMessage() {
        givenRebuiltMessage(message = null)
    }

    private fun givenRebuiltMessage(message: MessageData?) {
        coEvery {
            createForwardedMessage(
                conversationId = CONVERSATION_ID,
                messageId = MessageId(MESSAGE_ID_VALUE),
            )
        } returns message
    }

    private fun createViewModel(
        applicationScope: CoroutineScope = CoroutineScope(mainDispatcherRule.testDispatcher),
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            mapOf(
                FORWARD_CONVERSATION_ID_ARG to CONVERSATION_ID_VALUE,
                FORWARD_MESSAGE_ID_ARG to MESSAGE_ID_VALUE,
            ),
        ),
    ): ForwardMessageViewModel {
        return ForwardMessageViewModel(
            applicationScope = applicationScope,
            mainDispatcher = mainDispatcherRule.testDispatcher,
            conversationDraftLauncher = conversationDraftLauncher,
            sendContentToTargets = sendContentToTargets,
            buildConversationDraftFromMessage = buildConversationDraftFromMessage,
            createForwardedMessage = createForwardedMessage,
            savedStateHandle = savedStateHandle,
        )
    }

    private companion object {
        const val MESSAGE_ID_VALUE = "message-1"
        val TARGET_CONVERSATION_ID = ConversationId("conversation-2")
        val FORWARDED_DRAFT = ConversationDraft(messageText = "forwarded text")
        val TARGETS = setOf<SendTarget>(SendTarget.Conversation(TARGET_CONVERSATION_ID))
    }
}
