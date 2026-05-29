package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.groupState
import com.android.messaging.ui.conversationsettings.screen.support.oneToOneState
import io.mockk.verify
import org.junit.Test

internal class NotificationsClickTest : ConversationSettingsTestBase() {

    @Test
    fun notificationsRow_isDisplayed() {
        renderScreen(oneToOneState())

        composeTestRule
            .onNodeWithText(string(R.string.notifications_enabled_conversation_pref_title))
            .assertIsDisplayed()
    }

    @Test
    fun notificationsClick_dispatchesNotificationsClicked() {
        renderScreen(groupState())

        composeTestRule
            .onNodeWithText(string(R.string.notifications_enabled_conversation_pref_title))
            .performClick()

        verify(exactly = 1) {
            screenModel.onAction(ConversationSettingsAction.NotificationsClicked)
        }
    }
}
