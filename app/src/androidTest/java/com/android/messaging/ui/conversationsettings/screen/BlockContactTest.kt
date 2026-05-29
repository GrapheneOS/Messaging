package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.TEST_DESTINATION
import com.android.messaging.ui.conversationsettings.screen.support.groupState
import com.android.messaging.ui.conversationsettings.screen.support.oneToOneState
import com.android.messaging.ui.conversationsettings.screen.support.participant
import io.mockk.verify
import org.junit.Test

internal class BlockContactTest : ConversationSettingsTestBase() {

    @Test
    fun hidden_forGroupConversation() {
        renderScreen(groupState())

        composeTestRule
            .onNodeWithText(string(R.string.block_contact_title, ""), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun shown_forOneToOneConversation() {
        renderScreen(
            oneToOneState(
                otherParticipant = participant(displayDestination = TEST_DESTINATION),
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.block_contact_title, TEST_DESTINATION))
            .assertIsDisplayed()
    }

    @Test
    fun unblockTitle_whenAlreadyBlocked_andClickDispatchesUnblock() {
        renderScreen(
            oneToOneState(
                otherParticipant = participant(
                    isBlocked = true,
                    displayDestination = TEST_DESTINATION,
                ),
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.unblock_contact_title, TEST_DESTINATION))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(ConversationSettingsAction.UnblockClicked)
        }
    }

    @Test
    fun clickShowsConfirmation_okDispatchesBlockConfirmed() {
        renderScreen(
            oneToOneState(
                otherParticipant = participant(displayDestination = TEST_DESTINATION),
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.block_contact_title, TEST_DESTINATION))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(string(R.string.block_confirmation_message))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(string(android.R.string.ok))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(ConversationSettingsAction.BlockConfirmed)
        }
    }

    @Test
    fun clickShowsConfirmation_cancelDoesNothing() {
        renderScreen(
            oneToOneState(
                otherParticipant = participant(displayDestination = TEST_DESTINATION),
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.block_contact_title, TEST_DESTINATION))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(string(android.R.string.cancel))
            .performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 0) {
            screenModel.onAction(ConversationSettingsAction.BlockConfirmed)
        }
    }
}
