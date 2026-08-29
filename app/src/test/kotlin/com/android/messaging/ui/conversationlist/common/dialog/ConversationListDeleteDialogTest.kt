package com.android.messaging.ui.conversationlist.common.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Deleting a conversation is irreversible and is not covered by the archive undo snackbar, so it
 * has to carry at least the warning that deleting a single message already carries.
 */
@RunWith(RobolectricTestRunner::class)
internal class ConversationListDeleteDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deletingOneConversationWarnsThatItCannotBeUndone() {
        setDeleteDialogContent(selectedCount = 1)

        composeTestRule
            .onNodeWithText(string(R.string.delete_message_confirmation_dialog_text))
            .assertIsDisplayed()
    }

    @Test
    fun deletingSeveralConversationsWarnsThatItCannotBeUndone() {
        setDeleteDialogContent(selectedCount = 3)

        composeTestRule
            .onNodeWithText(string(R.string.delete_message_confirmation_dialog_text))
            .assertIsDisplayed()
    }

    private fun setDeleteDialogContent(selectedCount: Int) {
        composeTestRule.setContent {
            AppTheme {
                ConversationListDeleteDialog(
                    selectedCount = selectedCount,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
    }

    private fun string(resId: Int): String {
        return targetContext.getString(resId)
    }
}
