package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction
import com.android.messaging.ui.blockedparticipants.screen.support.BlockedParticipantsTestBase
import com.android.messaging.ui.blockedparticipants.screen.support.DESTINATION_1
import com.android.messaging.ui.blockedparticipants.screen.support.DESTINATION_2
import com.android.messaging.ui.blockedparticipants.screen.support.DISPLAY_NAME_1
import com.android.messaging.ui.blockedparticipants.screen.support.DISPLAY_NAME_2
import com.android.messaging.ui.blockedparticipants.screen.support.emptyState
import com.android.messaging.ui.blockedparticipants.screen.support.loadedState
import com.android.messaging.ui.blockedparticipants.screen.support.participant
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

internal class UnblockParticipantTest : BlockedParticipantsTestBase() {

    @Test
    fun emptyState_shown_whenNoParticipants() {
        renderScreen(emptyState())

        composeTestRule
            .onNodeWithText(string(R.string.blocked_contacts_empty))
            .assertIsDisplayed()
    }

    @Test
    fun participantRows_render_displayNames() {
        renderScreen(loadedState())

        composeTestRule.onNodeWithText(DISPLAY_NAME_1).assertIsDisplayed()
        composeTestRule.onNodeWithText(DISPLAY_NAME_2).assertIsDisplayed()
    }

    @Test
    fun unblockClick_dispatchesUnblockClicked_withNormalizedDestination() {
        renderScreen(
            loadedState(
                participants = persistentListOf(
                    participant(displayName = DISPLAY_NAME_1, destination = DESTINATION_1),
                ),
            ),
        )

        composeTestRule
            .onAllNodesWithContentDescription(string(R.string.tap_to_unblock_message))
            .onFirst()
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(BlockedParticipantsAction.UnblockClicked(DESTINATION_1))
        }
    }

    @Test
    fun unblockClick_perRow_passesCorrectDestination() {
        renderScreen(
            loadedState(
                participants = persistentListOf(
                    participant(
                        participantId = "p1",
                        displayName = DISPLAY_NAME_1,
                        destination = DESTINATION_1,
                    ),
                    participant(
                        participantId = "p2",
                        displayName = DISPLAY_NAME_2,
                        destination = DESTINATION_2,
                    ),
                ),
            ),
        )

        composeTestRule
            .onAllNodesWithContentDescription(string(R.string.tap_to_unblock_message))[1]
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(BlockedParticipantsAction.UnblockClicked(DESTINATION_2))
        }
    }
}
