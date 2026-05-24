package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantConversationSettingsAction as ParticipantAction
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.FATHER_DESTINATION
import com.android.messaging.ui.conversationsettings.screen.support.FATHER_NAME
import com.android.messaging.ui.conversationsettings.screen.support.MOTHER_DESTINATION
import com.android.messaging.ui.conversationsettings.screen.support.MOTHER_NAME
import com.android.messaging.ui.conversationsettings.screen.support.TEST_DESTINATION
import com.android.messaging.ui.conversationsettings.screen.support.groupState
import com.android.messaging.ui.conversationsettings.screen.support.oneToOneState
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

internal class ParticipantsListTest : ConversationSettingsTestBase() {

    @Test
    fun displaysAllParticipants_inGroup() {
        renderScreen(groupState())

        composeTestRule.onNodeWithText(string(R.string.participant_list_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(MOTHER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(FATHER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(MOTHER_DESTINATION).assertIsDisplayed()
        composeTestRule.onNodeWithText(FATHER_DESTINATION).assertIsDisplayed()
    }

    @Test
    fun click_opensQuickActionsPopup_inGroup() {
        renderScreen(groupState())

        composeTestRule.onNodeWithText(MOTHER_NAME).performClick()

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_send_message))
            .assertIsDisplayed()
    }

    @Test
    fun click_opensQuickActionsPopup_inOneToOne() {
        renderScreen(oneToOneState())

        composeTestRule.onNodeWithText(TEST_DESTINATION).performClick()

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_send_message))
            .assertIsDisplayed()
    }

    @Test
    fun quickActions_messageClick_dispatchesParticipantPressed() {
        renderScreen(groupState())

        composeTestRule.onNodeWithText(MOTHER_NAME).performClick()
        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_send_message))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(
                ParticipantAction.ParticipantPressed(MOTHER_DESTINATION),
            )
        }
    }

    @Test
    fun longPress_dispatchesParticipantLongPressed_withDetails() {
        renderScreen(groupState())

        composeTestRule.onNodeWithText(FATHER_NAME).performTouchInput { longClick() }

        verify(exactly = 1) {
            screenModel.onAction(
                ParticipantAction.ParticipantLongPressed(FATHER_DESTINATION),
            )
        }
    }

    @Test
    fun trailingInfoButton_visibleForEachParticipant_inGroup() {
        renderScreen(groupState())

        composeTestRule
            .onAllNodesWithContentDescription(string(R.string.action_contact_info))
            .assertCountEquals(2)
    }

    @Test
    fun trailingInfoButton_hidden_inOneToOne() {
        renderScreen(oneToOneState(canShowContact = false))

        composeTestRule
            .onAllNodesWithContentDescription(string(R.string.action_contact_info))
            .assertCountEquals(0)
    }

    @Test
    fun participantsSection_hidden_whenEmpty() {
        renderScreen(
            oneToOneState().copy(participants = persistentListOf()),
        )

        composeTestRule
            .onNodeWithText(string(R.string.participant_list_title))
            .assertDoesNotExist()
    }
}
