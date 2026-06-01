package com.android.messaging.ui.conversation.screen.simselector

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.ui.conversation.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG
import com.android.messaging.ui.conversation.conversationSimSelectorItemTestTag
import com.android.messaging.ui.conversation.testAttSubscription
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenSimSelectorIntegrationTest :
    BaseConversationScreenSimSelectorIntegrationTest() {

    @Test
    fun topBarSimSelector_whenAvailableOpensSheetAndSelectingSimDismissesIt() {
        val screenModel = createScreenModel()
        setSimSelectorContent(
            screenModel = screenModel,
            simSelector = createMultiSimSelector(),
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .assertIsDisplayed()
            .performClick()
        waitForSheetNodeCount(expectedCount = 1)

        composeTestRule
            .onNodeWithTag(
                conversationSimSelectorItemTestTag(
                    selfParticipantId = testAttSubscription.selfParticipantId,
                ),
            )
            .performClick()
        waitForSheetNodeCount(expectedCount = 0)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onSimSelected(
                    selfParticipantId = testAttSubscription.selfParticipantId,
                )
            }
        }
    }

    @Test
    fun openSimSelector_whenSelectorBecomesUnavailableDismissesSheet() {
        val screenModel = createScreenModel()
        setSimSelectorContent(
            screenModel = screenModel,
            simSelector = createMultiSimSelector(),
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG)
            .assertIsDisplayed()
            .performClick()
        waitForSheetNodeCount(expectedCount = 1)

        screenModel.scaffoldUiStateFlow.value = screenModel.scaffoldUiStateFlow.value.let {
            it.copy(
                composer = it.composer.copy(
                    simSelector = createSingleSimSelector(),
                ),
            )
        }

        waitForSheetNodeCount(expectedCount = 0)
    }
}
