package com.android.messaging.ui.conversation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.common.test.rules.AppTestRule
import com.android.common.test.rules.MessagingTestRule
import com.android.messaging.R
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversationlist.ConversationListActivity
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
    val composeTestRule = createEmptyComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun conversationListToConversation_newScreenUiElementsArePresent() {
        val scenario = ActivityScenario.launch(
            ConversationListActivity::class.java,
        )

        scenario.use {
            onView(withId(android.R.id.list))
                .check(matches(isDisplayed()))

            onView(withId(R.id.start_new_conversation_button))
                .check(matches(isDisplayed()))

            onView(withId(android.R.id.list))
                .perform(
                    RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                        0,
                        click(),
                    ),
                )

            composeTestRule.waitUntilAtLeastOneExists(
                matcher = hasTestTag(testTag = CONVERSATION_TEXT_FIELD_TEST_TAG),
                timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS,
            )

            composeTestRule
                .onNodeWithTag(testTag = CONVERSATION_MESSAGES_LIST_TEST_TAG)
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithTag(testTag = CONVERSATION_COMPOSE_BAR_TEST_TAG)
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithTag(testTag = CONVERSATION_TEXT_FIELD_TEST_TAG)
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithTag(
                    testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                    useUnmergedTree = true,
                )
                .assertIsDisplayed()

            pressBackUnconditionally()

            onView(withId(android.R.id.list))
                .check(matches(isDisplayed()))
        }
    }
}
