package com.android.messaging.ui.conversation.screen.simselector

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.android.messaging.ui.conversation.CONVERSATION_SEND_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG
import com.android.messaging.ui.conversation.conversationSimSelectorItemTestTag
import com.android.messaging.ui.conversation.testAttSubscription
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenSendButtonSimSelectorIntegrationTest :
    BaseConversationScreenSimSelectorIntegrationTest() {

    @Test
    fun sendLongPress_whenSimSelectorUnavailableDoesNotOpenSheet() {
        val screenModel = createScreenModel()
        setSimSelectorContent(
            screenModel = screenModel,
            simSelector = createSingleSimSelector(),
        )

        longClickSendButton()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun sendLongPress_whenSimSelectorAvailableOpensSheetAndSelectingSimDismissesIt() {
        val screenModel = createScreenModel()
        setSimSelectorContent(
            screenModel = screenModel,
            simSelector = createMultiSimSelector(),
        )

        longClickSendButton()
        waitForSheetNodeCount(expectedCount = 1)
        composeTestRule
            .onNodeWithTag(CONVERSATION_SIM_SELECTOR_SHEET_TEST_TAG)
            .assertIsDisplayed()

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

    private fun longClickSendButton() {
        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_SEND_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performTouchInput {
                longClick(position = center)
            }
        composeTestRule.waitForIdle()
    }
}
