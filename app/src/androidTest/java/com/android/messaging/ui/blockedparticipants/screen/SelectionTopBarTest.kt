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
import com.android.messaging.ui.blockedparticipants.screen.support.PARTICIPANT_ID_3
import com.android.messaging.ui.blockedparticipants.screen.support.loadedState
import com.android.messaging.ui.blockedparticipants.screen.support.stateWithSelection
import io.mockk.verify
import org.junit.Test

internal class SelectionTopBarTest : BlockedParticipantsTestBase() {

    @Test
    fun defaultTitle_shown_whenNothingSelected() {
        renderScreen(loadedState())

        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.back))
            .assertIsDisplayed()
    }

    @Test
    fun selectionTitle_showsCount_whenOneSelected() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_selection_title, 1))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_title))
            .assertDoesNotExist()
    }

    @Test
    fun selectionTitle_updates_whenSelectionGrows() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_selection_title, 1))
            .assertIsDisplayed()

        updateState(stateWithSelection(setOf(PARTICIPANT_ID_1, PARTICIPANT_ID_2, PARTICIPANT_ID_3)))

        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_selection_title, 3))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_selection_title, 1))
            .assertDoesNotExist()
    }

    @Test
    fun clearSelectionButton_shown_inSelectionMode() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        composeTestRule
            .onNodeWithContentDescription(
                string(R.string.blocked_contacts_clear_selection_content_description),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.back))
            .assertDoesNotExist()
    }

    @Test
    fun clearSelectionClick_dispatchesClearSelectionClicked() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1, PARTICIPANT_ID_2)))

        composeTestRule
            .onNodeWithContentDescription(
                string(R.string.blocked_contacts_clear_selection_content_description),
            )
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(BlockedParticipantsAction.ClearSelectionClicked)
        }
    }

    @Test
    fun deleteAction_hidden_whenNothingSelected() {
        renderScreen(loadedState())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .assertDoesNotExist()
    }

    @Test
    fun deleteAction_shown_inSelectionMode() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .assertIsDisplayed()
    }

    @Test
    fun leavingSelectionMode_restoresDefaultTopBar() {
        renderScreen(stateWithSelection(setOf(PARTICIPANT_ID_1)))

        updateState(loadedState())

        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.back))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_delete))
            .assertDoesNotExist()
    }
}
