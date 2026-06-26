package com.android.messaging.ui.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.common.test.rules.AppTestRule
import com.android.common.test.rules.MessagingTestRule
import com.android.messaging.ui.conversationlist.ConversationListActivity
import com.android.messaging.ui.conversationlist.common.CONVERSATION_LIST_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationUserFlowTest {

    @get:Rule
    val appRule = AppTestRule()

    @get:Rule
    val messagingRule = MessagingTestRule()

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun conversationListToConversation_newScreenUiElementsArePresent() {
        val scenario = ActivityScenario.launch(
            ConversationListActivity::class.java,
        )

        scenario.use {
            composeRule.waitUntilAtLeastOneExists(
                matcher = hasTestTag(testTag = CONVERSATION_LIST_TEST_TAG),
                timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS,
            )

            composeRule
                .onNodeWithTag(testTag = CONVERSATION_LIST_TEST_TAG)
                .assertIsDisplayed()

            composeRule
                .onNodeWithTag(testTag = CONVERSATION_LIST_TEST_TAG)
                .onChildren()
                .onFirst()
                .performClick()

            composeRule.waitUntilAtLeastOneExists(
                matcher = hasTestTag(testTag = CONVERSATION_TEXT_FIELD_TEST_TAG),
                timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS,
            )

            composeRule
                .onNodeWithTag(testTag = CONVERSATION_MESSAGES_LIST_TEST_TAG)
                .assertIsDisplayed()

            composeRule
                .onNodeWithTag(testTag = CONVERSATION_COMPOSE_BAR_TEST_TAG)
                .assertIsDisplayed()

            composeRule
                .onNodeWithTag(testTag = CONVERSATION_TEXT_FIELD_TEST_TAG)
                .assertIsDisplayed()

            composeRule
                .onNodeWithTag(
                    testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                    useUnmergedTree = true,
                )
                .assertIsDisplayed()
        }
    }

    private companion object {
        private const val TEST_WAIT_TIMEOUT_MILLIS = 5_000L
    }
}
