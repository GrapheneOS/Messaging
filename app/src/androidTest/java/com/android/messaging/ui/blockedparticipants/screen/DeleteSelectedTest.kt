package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction
import com.android.messaging.ui.blockedparticipants.screen.support.BlockedParticipantsTestBase
import com.android.messaging.ui.blockedparticipants.screen.support.PARTICIPANT_ID_1
import com.android.messaging.ui.blockedparticipants.screen.support.PARTICIPANT_ID_2
import com.android.messaging.ui.blockedparticipants.screen.support.stateWithSelection
import io.mockk.verify
import org.junit.Test

internal class DeleteSelectedTest : BlockedParticipantsTestBase() {

    @Test
    fun deleteClick_showsConfirmationDialog_withSingularTitle() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .performClick()

        composeTestRule
            .onNodeWithText(
                quantityString(R.plurals.delete_conversations_confirmation_dialog_title, 1),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.delete_message_confirmation_dialog_text))
            .assertIsDisplayed()
    }

    @Test
    fun deleteClick_showsConfirmationDialog_withPluralTitle() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1, PARTICIPANT_ID_2)))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .performClick()

        composeTestRule
            .onNodeWithText(
                quantityString(R.plurals.delete_conversations_confirmation_dialog_title, 2),
            )
            .assertIsDisplayed()
    }

    @Test
    fun confirmDelete_dispatchesDeleteSelectedConfirmed() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1, PARTICIPANT_ID_2)))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .performClick()

        composeTestRule
            .onNodeWithText(string(R.string.delete_conversation_confirmation_button))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(BlockedParticipantsAction.DeleteSelectedConfirmed)
        }
    }

    @Test
    fun confirmDelete_dismissesDialog() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.delete_conversation_confirmation_button))
            .performClick()

        composeTestRule
            .onNodeWithText(string(R.string.delete_message_confirmation_dialog_text))
            .assertDoesNotExist()
    }

    @Test
    fun cancelDelete_doesNotDispatchAction_andDismissesDialog() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .performClick()
        composeTestRule
            .onNodeWithText(string(R.string.delete_conversation_decline_button))
            .performClick()

        verify(exactly = 0) {
            screenModel.onAction(BlockedParticipantsAction.DeleteSelectedConfirmed)
        }
        composeTestRule
            .onNodeWithText(string(R.string.delete_message_confirmation_dialog_text))
            .assertDoesNotExist()
    }
}
