package com.android.messaging.ui.conversation.screen.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_DELETE_MESSAGES_CONFIRM_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_DELETE_MESSAGES_DISMISS_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.screen.model.ConversationMessageDeleteConfirmationUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenDeleteDialogsTest : BaseConversationScreenDialogsTest() {

    @Test
    fun noDialogState_rendersNoDialogText() {
        setDialogsContent(uiState = createDialogUiState())

        composeTestRule
            .onNodeWithText(text(R.string.mms_attachment_limit_reached))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(deleteConversationTitle())
            .assertDoesNotExist()
    }

    @Test
    fun deleteConversation_confirmAndDismissForwardCallbacks() {
        val screenModel = setDialogsContent(
            uiState = createDialogUiState(
                isDeleteConversationConfirmationVisible = true,
            ),
        )

        composeTestRule
            .onNodeWithText(deleteConversationTitle())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text(R.string.delete_conversation_decline_button))
            .performClick()
        composeTestRule
            .onNodeWithText(text(R.string.delete_conversation_confirmation_button))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.dismissDeleteConversationConfirmation()
            }
            verify(exactly = 1) {
                screenModel.confirmDeleteConversation()
            }
        }
    }

    @Test
    fun deleteMessages_multiMessageUsesPluralAndForwardsCallbacks() {
        val messageIds = persistentSetOf("message-1", "message-2", "message-3")
        val screenModel = setDialogsContent(
            uiState = createDialogUiState(
                deleteConfirmation = ConversationMessageDeleteConfirmationUiState(
                    messageIds = messageIds,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(
                quantityText(
                    resourceId = R.plurals.delete_messages_confirmation_dialog_title,
                    quantity = messageIds.size,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text(R.string.delete_message_confirmation_dialog_text))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_DELETE_MESSAGES_DISMISS_BUTTON_TEST_TAG)
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_DELETE_MESSAGES_CONFIRM_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.dismissDeleteMessageConfirmation()
            }
            verify(exactly = 1) {
                screenModel.confirmDeleteSelectedMessages()
            }
        }
    }
}
