package com.android.messaging.ui.conversation.screen.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenSubjectDialogTest : BaseConversationScreenDialogsTest() {

    @Test
    fun subjectDialog_prefillsFocusesClearsAndConfirmsText() {
        val screenModel = setDialogsContent(
            uiState = createDialogUiState(
                isSubjectDialogVisible = true,
                subjectText = INITIAL_SUBJECT,
            ),
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_SUBJECT_DIALOG_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text(R.string.subject_dialog_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG)
            .assertTextEquals(INITIAL_SUBJECT)
            .assertIsFocused()
        composeTestRule
            .onNodeWithTag(CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG)
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG)
            .performTextInput(UPDATED_SUBJECT)
        composeTestRule
            .onNodeWithTag(CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(okText())
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onSubjectDialogConfirm(subjectText = UPDATED_SUBJECT)
            }
            verify(exactly = 0) {
                screenModel.onSubjectDialogDismiss()
            }
        }
    }

    private companion object {
        private const val INITIAL_SUBJECT = "Weekend"
        private const val UPDATED_SUBJECT = "Dinner plan"
    }
}
