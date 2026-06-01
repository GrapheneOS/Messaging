package com.android.messaging.ui.conversation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationActivityRecipientPickerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ConversationActivity>()

    @Test
    fun createGroupAction_keepsUserOnNewChatScreenAndShowsInlineSelectionMode() {
        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_TOP_APP_BAR_TITLE_TEST_TAG)
            .assertTextEquals(
                targetContext.getString(R.string.conversation_new_group),
            )
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(testTag = NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG)
            .assertCountEquals(expectedSize = 0)

        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_NAVIGATE_BACK_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_TOP_APP_BAR_TITLE_TEST_TAG)
            .assertTextEquals(
                targetContext.getString(R.string.start_new_conversation),
            )
            .assertIsDisplayed()
    }
}
