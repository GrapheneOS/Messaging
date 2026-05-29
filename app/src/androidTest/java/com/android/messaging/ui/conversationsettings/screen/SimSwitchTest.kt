package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.SUB1_DESTINATION
import com.android.messaging.ui.conversationsettings.screen.support.SUB1_ID
import com.android.messaging.ui.conversationsettings.screen.support.SUB1_SLOT
import com.android.messaging.ui.conversationsettings.screen.support.SUB2_DESTINATION
import com.android.messaging.ui.conversationsettings.screen.support.SUB2_ID
import com.android.messaging.ui.conversationsettings.screen.support.SUB2_SLOT
import com.android.messaging.ui.conversationsettings.screen.support.oneToOneState
import com.android.messaging.ui.conversationsettings.screen.support.subscription
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

internal class SimSwitchTest : ConversationSettingsTestBase() {

    @Test
    fun hidden_whenNotAvailable() {
        renderScreen(oneToOneState(isSimSwitchAvailable = false))

        composeTestRule
            .onNodeWithText(string(R.string.sim_selector_item_title))
            .assertDoesNotExist()
    }

    @Test
    fun displaysSelectedSubscription() {
        val sub1 = subscription(SUB1_ID, SUB1_SLOT, SUB1_DESTINATION)
        val sub2 = subscription(SUB2_ID, SUB2_SLOT, SUB2_DESTINATION)

        renderScreen(
            oneToOneState(
                isSimSwitchAvailable = true,
                availableSubscriptions = persistentListOf(sub1, sub2),
                selectedSubscription = sub1,
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.sim_selector_item_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(SUB1_DESTINATION).assertIsDisplayed()
    }

    @Test
    fun selectingOtherSim_dispatchesSimSelected() {
        val sub1 = subscription(SUB1_ID, SUB1_SLOT, SUB1_DESTINATION)
        val sub2 = subscription(SUB2_ID, SUB2_SLOT, SUB2_DESTINATION)

        renderScreen(
            oneToOneState(
                isSimSwitchAvailable = true,
                availableSubscriptions = persistentListOf(sub1, sub2),
                selectedSubscription = sub1,
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.sim_selector_item_title))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(SUB2_DESTINATION).performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.onAction(
                ConversationSettingsAction.SimSelected(SUB2_ID),
            )
        }
    }
}
