package com.android.messaging.ui.conversation.screen.dialogs

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversation.screen.model.ConversationAttachmentLimitWarning
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenAttachmentLimitDialogTest : BaseConversationScreenDialogsTest() {

    @Test
    fun composingAttachmentLimit_showsDismissOnlyMessage() {
        val screenModel = setDialogsContent(
            uiState = createDialogUiState(
                attachmentLimitWarning =
                    ConversationAttachmentLimitWarning.ComposingAttachmentLimitReached,
            ),
        )

        composeTestRule
            .onNodeWithText(text(R.string.mms_attachment_limit_reached))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text(R.string.attachment_limit_reached_dialog_message_when_composing))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text(R.string.attachment_limit_reached_send_anyway))
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onNodeWithText(okText())
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.dismissAttachmentLimitWarning()
            }
            verify(exactly = 0) {
                screenModel.sendAnywayAfterAttachmentLimitWarning()
            }
        }
    }

    @Test
    fun sendingMessageLimit_showsSendAnywayAndDismissActions() {
        val screenModel = setDialogsContent(
            uiState = createDialogUiState(
                attachmentLimitWarning =
                    ConversationAttachmentLimitWarning.SendingMessageLimitReached,
            ),
        )

        composeTestRule
            .onNodeWithText(text(R.string.attachment_limit_reached_dialog_message_when_sending))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text(R.string.attachment_limit_reached_send_anyway))
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithText(okText())
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.sendAnywayAfterAttachmentLimitWarning()
            }
            verify(exactly = 1) {
                screenModel.dismissAttachmentLimitWarning()
            }
        }
    }

    @Test
    fun sendingVideoLimit_showsVideoSpecificDismissOnlyMessage() {
        val screenModel = setDialogsContent(
            uiState = createDialogUiState(
                attachmentLimitWarning =
                    ConversationAttachmentLimitWarning.SendingVideoAttachmentLimitReached,
            ),
        )

        composeTestRule
            .onNodeWithText(text(R.string.video_attachment_limit_exceeded_when_sending))
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(text(R.string.attachment_limit_reached_send_anyway))
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onNodeWithText(okText())
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.dismissAttachmentLimitWarning()
            }
            verify(exactly = 0) {
                screenModel.sendAnywayAfterAttachmentLimitWarning()
            }
        }
    }
}
