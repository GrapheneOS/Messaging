package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG
import com.android.messaging.ui.conversation.TEST_ATT_SUBSCRIPTION_NAME
import com.android.messaging.ui.conversation.TEST_VERIZON_SUBSCRIPTION_NAME
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.conversationSimSelectorItemTestTag
import com.android.messaging.ui.conversation.testAttSubscription
import com.android.messaging.ui.conversation.testVerizonSubscription
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationSimSelectorSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sheet_rendersTitleAndAllSubscriptions() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = testVerizonSubscription,
            ),
        )

        val title = targetContext.getString(R.string.sim_selector_sheet_title)

        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(TEST_VERIZON_SUBSCRIPTION_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(TEST_ATT_SUBSCRIPTION_NAME).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testVerizonSubscription.displayDestination.orEmpty())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(testAttSubscription.displayDestination.orEmpty())
            .assertIsDisplayed()
    }

    @Test
    fun sheet_marksSelectedSubscriptionWithCheckIcon() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = testAttSubscription,
            ),
        )

        val selectedDescription = targetContext.getString(R.string.sim_selector_item_selected)

        composeTestRule
            .onNodeWithContentDescription(selectedDescription)
            .assertIsDisplayed()
    }

    @Test
    fun sheet_doesNotShowCheckWhenNoSubscriptionIsSelected() {
        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = null,
            ),
        )

        val selectedDescription = targetContext.getString(R.string.sim_selector_item_selected)

        composeTestRule
            .onNodeWithContentDescription(selectedDescription)
            .assertDoesNotExist()
    }

    @Test
    fun sheet_invokesCallbackWithSelfParticipantIdWhenRowClicked() {
        val selections = mutableListOf<String>()

        setContent(
            uiState = ConversationSimSelectorUiState(
                subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                selectedSubscription = testVerizonSubscription,
            ),
            onSimSelected = { selfParticipantId ->
                selections += selfParticipantId
            },
        )

        composeTestRule
            .onNodeWithTag(
                conversationSimSelectorItemTestTag(
                    selfParticipantId = testAttSubscription.selfParticipantId
                ),
            )
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(testAttSubscription.selfParticipantId), selections)
        }
    }

    private fun setContent(
        uiState: ConversationSimSelectorUiState,
        onSimSelected: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationSimSelectorSheet(
                    uiState = uiState,
                    onSimSelected = onSimSelected,
                    onDismissRequest = {},
                )
            }
        }
    }
}
