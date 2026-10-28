package com.android.messaging.ui.conversation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A window around an older message, which stops short of the newest message. */
@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenNewerMessagesTest : BaseConversationScreenTest() {

    @Test
    fun windowStoppingShortOfTheNewestMessage_nearItsEnd_asksForNewerMessages() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = windowMessages(count = 50),
        ).withNewerMessages(latestMessage = latestMessage(index = 1, isIncoming = false))

        setContent(screenModel = screenModel.model)
        composeTestRule.waitForIdle()

        verify(atLeast = 1) {
            screenModel.model.onLoadNewerMessages()
        }
    }

    @Test
    fun windowHoldingTheNewestMessage_nearItsEnd_doesNotAskForNewerMessages() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = windowMessages(count = 50),
        )

        setContent(screenModel = screenModel.model)
        composeTestRule.waitForIdle()

        verify(exactly = 0) {
            screenModel.model.onLoadNewerMessages()
        }
    }

    /** The scroll to the target starts from the end of the window, which is not worth growing. */
    @Test
    fun pendingTargetFarFromTheEndOfTheWindow_doesNotAskForNewerMessages() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = windowMessages(count = 300),
        ).withNewerMessages(latestMessage = latestMessage(index = 1, isIncoming = false))

        setContent(
            screenModel = screenModel.model,
            pendingScrollMessageId = MessageId("message-100"),
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("message-100")))
            .assertIsDisplayed()
        verify(exactly = 0) {
            screenModel.model.onLoadNewerMessages()
        }
    }

    @Test
    fun incomingMessagePastTheWindow_offersTheNewestMessagesWithoutMovingTheList() {
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(messages = windowMessages(count = 50))
        screenModel.scaffoldUiStateFlow.value = uiState.withNewerMessages(
            latestMessage = latestMessage(index = 1, isIncoming = true),
        )
        setContent(screenModel = screenModel.model)
        composeTestRule.waitForIdle()

        screenModel.scaffoldUiStateFlow.value = uiState.withNewerMessages(
            latestMessage = latestMessage(index = 2, isIncoming = true),
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("message-50")))
            .assertIsDisplayed()
        verify(exactly = 0) {
            screenModel.model.onLoadNewestMessages()
        }

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(
                    R.string.in_conversation_notify_new_message_action,
                ),
            )
            .performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.model.onLoadNewestMessages()
        }
    }

    @Test
    fun ownMessagePastTheWindow_showsTheNewestMessagesFromTheirEnd() {
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(messages = windowMessages(count = 50))
        screenModel.scaffoldUiStateFlow.value = uiState.withNewerMessages(
            latestMessage = latestMessage(index = 1, isIncoming = false),
        )
        setContent(screenModel = screenModel.model)
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)
        composeTestRule.waitForIdle()

        screenModel.scaffoldUiStateFlow.value = uiState.withNewerMessages(
            latestMessage = latestMessage(index = 2, isIncoming = false),
        )
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.model.onLoadNewestMessages()
        }

        // What the delegate swaps in: a window moved elsewhere, none of whose messages the list
        // could keep its place by.
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 50,
                latestMessageId = "latest-50",
                latestMessageIncoming = false,
                messageIdPrefix = "latest",
            ),
        ).withWindowId(windowId = WINDOW_AROUND_THE_TARGET_ID + 1)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("latest-50")))
            .assertIsDisplayed()
    }

    private fun windowMessages(count: Int): List<ConversationMessageUiModel> {
        return createMessages(
            count = count,
            latestMessageId = "message-$count",
            latestMessageIncoming = false,
        )
    }

    private fun latestMessage(index: Int, isIncoming: Boolean): ConversationMessageUiModel {
        return createMessages(
            count = index,
            latestMessageId = "latest-$index",
            latestMessageIncoming = isIncoming,
            messageIdPrefix = "latest",
        ).last()
    }

    private fun ConversationScreenScaffoldUiState.withNewerMessages(
        latestMessage: ConversationMessageUiModel,
    ): ConversationScreenScaffoldUiState {
        return copy(
            messages = (messages as ConversationMessagesUiState.Present).copy(
                windowId = WINDOW_AROUND_THE_TARGET_ID,
                hasNewerMessages = true,
                latestUnloadedMessage = latestMessage,
            ),
        )
    }

    private fun ConversationScreenScaffoldUiState.withWindowId(
        windowId: Int,
    ): ConversationScreenScaffoldUiState {
        return copy(
            messages = (messages as ConversationMessagesUiState.Present).copy(windowId = windowId),
        )
    }

    private companion object {
        private const val WINDOW_AROUND_THE_TARGET_ID = 1
    }
}
