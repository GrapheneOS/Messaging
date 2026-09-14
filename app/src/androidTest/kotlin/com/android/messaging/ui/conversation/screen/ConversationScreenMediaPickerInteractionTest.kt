package com.android.messaging.ui.conversation.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationScreenMediaPickerInteractionTest : BaseConversationScreenTest() {

    @Test
    fun openingMediaPicker_hidesComposerAndShowsOverlay() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(
                testTag = CONVERSATION_ATTACHMENT_BUTTON_TEST_TAG,
                useUnmergedTree = true,
            )
            .performClick()
        composeTestRule
            .onNodeWithTag(CONVERSATION_ATTACHMENT_MEDIA_MENU_ITEM_TEST_TAG)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .assertCountEquals(expectedSize = 0)
        composeTestRule
            .onAllNodesWithTag(CONVERSATION_MEDIA_PICKER_OVERLAY_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
    }
}
