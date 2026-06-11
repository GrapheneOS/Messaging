package com.android.messaging.ui.conversation.screen.dialogs

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenSubjectDialogPlaceholderTest :
    BaseConversationScreenDialogsTest() {

    @Test
    fun emptySubjectDialog_showsPlaceholderWithoutClearActionAndDismisses() {
        val screenModel = setDialogsContent(
            uiState = createDialogUiState(
                isSubjectDialogVisible = true,
                subjectText = "",
            ),
        )

        composeTestRule
            .onAllNodesWithText(text(R.string.compose_message_view_subject_hint_text))
            .assertCountEquals(expectedSize = 2)
        val editableText = composeTestRule
            .onNodeWithTag(CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.EditableText)
            ?.text
        assertEquals("", editableText)
        composeTestRule
            .onNodeWithTag(CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(cancelText())
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onSubjectDialogDismiss()
            }
            verify(exactly = 0) {
                screenModel.onSubjectDialogConfirm(subjectText = any())
            }
        }
    }
}
