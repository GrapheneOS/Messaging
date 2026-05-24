package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantConversationSettingsAction
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.TEST_DESTINATION
import com.android.messaging.ui.conversationsettings.screen.support.groupState
import com.android.messaging.ui.conversationsettings.screen.support.oneToOneState
import com.android.messaging.ui.conversationsettings.screen.support.participant
import io.mockk.verify
import org.junit.Test

internal class ContactActionsTest : ConversationSettingsTestBase() {

    @Test
    fun callButton_hidden_whenCannotCall() {
        renderScreen(oneToOneState(canCall = false, canShowContact = true))

        composeTestRule
            .onNodeWithText(string(R.string.action_call))
            .assertDoesNotExist()
    }

    @Test
    fun callButton_displayed_andDispatchesCallClicked() {
        renderScreen(oneToOneState(canCall = true))

        val callLabel = string(R.string.action_call)
        composeTestRule.onNodeWithText(callLabel).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(callLabel).performClick()

        verify(exactly = 1) {
            screenModel.onAction(
                ParticipantConversationSettingsAction.ParticipantCallClicked(TEST_DESTINATION),
            )
        }
    }

    @Test
    fun contactInfoButton_hidden_whenCannotShowContact() {
        renderScreen(oneToOneState(canShowContact = false, canCall = true))

        composeTestRule
            .onNodeWithText(string(R.string.action_contact_info))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(R.string.action_add_contact))
            .assertDoesNotExist()
    }

    @Test
    fun contactInfoButton_showsAddContact_whenContactNotSaved() {
        renderScreen(oneToOneState(canShowContact = true, isContactSaved = false))

        composeTestRule
            .onNodeWithText(string(R.string.action_add_contact))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.action_contact_info))
            .assertDoesNotExist()
    }

    @Test
    fun contactInfoButton_showsContactInfo_whenContactSaved_andDispatchesAction() {
        val otherParticipant = participant()
        renderScreen(
            oneToOneState(
                canShowContact = true,
                isContactSaved = true,
                otherParticipant = otherParticipant,
            ),
        )

        composeTestRule
            .onNodeWithContentDescription(string(R.string.action_contact_info))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(
                ParticipantConversationSettingsAction.ParticipantContactInfoClicked(
                    otherParticipant,
                ),
            )
        }
    }

    @Test
    fun contactRow_hidden_forGroup() {
        renderScreen(groupState())

        composeTestRule
            .onNodeWithText(string(R.string.action_call))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(R.string.action_contact_info))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(R.string.action_add_contact))
            .assertDoesNotExist()
    }
}
