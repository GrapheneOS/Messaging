package com.android.messaging.ui.conversation.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageItemTestTag
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenScrollBehaviorTest : BaseConversationScreenTest() {

    @Test
    fun outgoingInsert_scrollsToLatestMessage() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "message-30",
                latestMessageIncoming = false,
            ),
        )

        setContent(screenModel = screenModel.model)
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)
        composeTestRule.waitForIdle()

        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 31,
                latestMessageId = "message-31",
                latestMessageIncoming = false,
            ),
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "message-31"))
            .assertIsDisplayed()
    }

    @Test
    fun conversationChange_resetsListStateToLatestMessage() {
        val screenModel = createScreenModel()
        var conversationState by mutableStateOf(
            Pair(
                CONVERSATION_ID,
                1,
            ),
        )
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "conversation-1-message-30",
                latestMessageIncoming = false,
                messageIdPrefix = "conversation-1-message",
            ),
        )

        setContent(
            screenModel = screenModel.model,
            conversationId = { conversationState.first },
            launchGeneration = { conversationState.second },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "conversation-1-message-30"))
            .assertDoesNotExist()

        composeTestRule.runOnIdle {
            conversationState = Pair(
                "conversation-2",
                2,
            )
            screenModel.scaffoldUiStateFlow.value = createPresentUiState(
                messages = createMessages(
                    count = 5,
                    latestMessageId = "conversation-2-message-5",
                    latestMessageIncoming = false,
                    messageIdPrefix = "conversation-2-message",
                ),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "conversation-2-message-5"))
            .assertIsDisplayed()
    }

    @Test
    fun pendingScrollPosition_anchorsTargetMessage_andInvokesConsumed() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 50,
                latestMessageId = "message-50",
                latestMessageIncoming = false,
            ),
        )
        var consumedCount = 0

        setContent(
            screenModel = screenModel.model,
            pendingScrollPosition = 5,
            onPendingScrollPositionConsumed = { consumedCount += 1 },
        )

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = "message-6"))
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(1, consumedCount)
        }
    }

    @Test
    fun nullPendingScrollPosition_doesNotInvokeConsumed() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 8,
                latestMessageId = "message-8",
                latestMessageIncoming = false,
            ),
        )
        var consumedCount = 0

        setContent(
            screenModel = screenModel.model,
            pendingScrollPosition = null,
            onPendingScrollPositionConsumed = { consumedCount += 1 },
        )

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(0, consumedCount)
        }
    }
}
