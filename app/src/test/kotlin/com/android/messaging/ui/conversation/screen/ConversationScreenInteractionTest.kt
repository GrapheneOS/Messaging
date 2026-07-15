package com.android.messaging.ui.conversation.screen

import android.Manifest
import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.every
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenInteractionTest : BaseConversationScreenTest() {

    @Test
    fun longPressingRecordButton_whenAudioPermissionGrantedStartsUnlockedRecording() {
        grantAudioRecordingPermission()
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
        )
        every { screenModel.model.tryStartAddingAttachment() } returns true
        screenModel.scaffoldUiStateFlow.value = uiState.copy(
            composer = uiState.composer.copy(
                isSendEnabled = false,
                isRecordActionEnabled = true,
                shouldShowRecordAction = true,
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_SEND_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                advanceEventTime(durationMillis = 700L)
                up()
            }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.tryStartAddingAttachment()
            }
            verify(exactly = 1) {
                screenModel.model.onAudioRecordingStart(isLocked = false)
            }
            verify(exactly = 0) {
                screenModel.model.onAudioRecordingStart(isLocked = true)
            }
        }
    }

    @Test
    fun clickingAudioAttachmentMenuItem_whenAudioPermissionGrantedStartsLockedRecording() {
        grantAudioRecordingPermission()
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
        )
        every { screenModel.model.tryStartAddingAttachment() } returns true
        screenModel.scaffoldUiStateFlow.value = uiState.copy(
            composer = uiState.composer.copy(
                isAttachmentActionEnabled = true,
                isRecordActionEnabled = true,
                messageText = "Message",
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_AUDIO_MENU_ITEM_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.tryStartAddingAttachment()
            }
            verify(exactly = 0) {
                screenModel.model.onAudioRecordingStart(isLocked = false)
            }
            verify(exactly = 1) {
                screenModel.model.onAudioRecordingStart(isLocked = true)
            }
        }
    }

    @Test
    fun longPressingMessage_forwardsLongClickToScreenModel() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(conversationMessageBubbleTestTag(messageId = "message-3"))
            .performSemanticsAction(SemanticsActions.OnLongClick)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageLongClick(messageId = "message-3")
            }
        }
    }

    @Test
    fun clickingMessageInSelectionMode_forwardsClickToScreenModel() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-3"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Copy,
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(conversationMessageSelectionRowTestTag(messageId = "message-2"))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageClick(messageId = "message-2")
            }
        }
    }

    @Test
    fun clickingFailedMessage_forwardsResendClickToScreenModel() {
        val screenModel = createScreenModel()
        val failedStatusText = targetContext.getString(R.string.message_status_send_failed)
        val messages = createMessages(
            count = 3,
            latestMessageId = "message-3",
            latestMessageIncoming = false,
        ).map { message ->
            when (message.messageId) {
                "message-2" -> {
                    message.copy(
                        status = ConversationMessageUiModel.Status.Outgoing.Failed,
                        canResendMessage = true,
                    )
                }

                else -> message
            }
        }

        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = messages,
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithText(failedStatusText, substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(conversationMessageBubbleTestTag(messageId = "message-2"))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageResendClick(messageId = "message-2")
            }
        }
    }

    private fun grantAudioRecordingPermission() {
        Shadows
            .shadowOf(targetContext as Application)
            .grantPermissions(Manifest.permission.RECORD_AUDIO)
    }
}
