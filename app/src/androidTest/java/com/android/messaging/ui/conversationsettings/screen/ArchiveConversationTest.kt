package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.oneToOneState
import io.mockk.verify
import org.junit.Test

internal class ArchiveConversationTest : ConversationSettingsTestBase() {

    @Test
    fun archiveTitle_shown_whenNotArchived() {
        renderScreen(oneToOneState(isArchived = false))

        composeTestRule
            .onNodeWithText(string(R.string.action_archive))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.action_unarchive))
            .assertDoesNotExist()
    }

    @Test
    fun unarchiveTitle_shown_whenArchived() {
        renderScreen(oneToOneState(isArchived = true))

        composeTestRule
            .onNodeWithText(string(R.string.action_unarchive))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.action_archive))
            .assertDoesNotExist()
    }

    @Test
    fun archiveClick_dispatchesArchiveClicked() {
        renderScreen(oneToOneState(isArchived = false))

        composeTestRule
            .onNodeWithText(string(R.string.action_archive))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(ConversationSettingsAction.ArchiveClicked)
        }
    }

    @Test
    fun unarchiveClick_dispatchesUnarchiveClicked() {
        renderScreen(oneToOneState(isArchived = true))

        composeTestRule
            .onNodeWithText(string(R.string.action_unarchive))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(ConversationSettingsAction.UnarchiveClicked)
        }
    }
}
