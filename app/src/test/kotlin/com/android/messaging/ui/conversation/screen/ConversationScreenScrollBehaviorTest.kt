package com.android.messaging.ui.conversation.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.conversationMessageItemTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.messages.ui.message.MessageHighlightDuration
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.conversation.testAttSubscription
import com.android.messaging.ui.conversation.testVerizonSubscription
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
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
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("message-31")))
            .assertIsDisplayed()
    }

    @Test
    fun conversationChange_resetsListStateToLatestMessage() {
        val screenModel = createScreenModel()
        var activeConversationId by mutableStateOf(CONVERSATION_ID)
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
            conversationId = { activeConversationId },
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performScrollToIndex(index = 20)
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(
                conversationMessageItemTestTag(messageId = MessageId("conversation-1-message-30"))
            )
            .assertDoesNotExist()

        composeTestRule.runOnIdle {
            activeConversationId = ConversationId("conversation-2")
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
            .onNodeWithTag(
                conversationMessageItemTestTag(messageId = MessageId("conversation-2-message-5"))
            )
            .assertIsDisplayed()
    }

    @Test
    fun pendingScrollMessageId_anchorsTargetMessage_andInvokesConsumed() {
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
            pendingScrollMessageId = MessageId("message-45"),
            onPendingScrollMessageIdConsumed = { consumedCount += 1 },
        )

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("message-45")))
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(1, consumedCount)
        }
        verify(exactly = 1) {
            screenModel.model.onTargetMessageShown(messageId = MessageId("message-45"))
        }
        verify(exactly = 0) {
            screenModel.model.onLoadMessage(messageId = any())
        }
    }

    @Test
    fun pendingScrollMessageId_insideTheWindow_centersTheTarget() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 50,
                latestMessageId = "message-50",
                latestMessageIncoming = false,
            ),
        )

        setContent(
            screenModel = screenModel.model,
            pendingScrollMessageId = MessageId("message-30"),
        )
        composeTestRule.waitForIdle()

        assertMessageIsCentered(messageId = MessageId("message-30"))
    }

    /** The line naming the outgoing SIM is a list item of its own, below the newest message. */
    @Test
    fun pendingScrollMessageId_withTheSendSimLineShown_centersTheTargetItself() {
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 50,
                latestMessageId = "message-50",
                latestMessageIncoming = false,
            ),
        )
        screenModel.scaffoldUiStateFlow.value = uiState.copy(
            composer = uiState.composer.copy(
                simSelector = ConversationSimSelectorUiState(
                    subscriptions = persistentListOf(testVerizonSubscription, testAttSubscription),
                    selectedSubscription = testVerizonSubscription,
                ),
            ),
        )

        setContent(
            screenModel = screenModel.model,
            pendingScrollMessageId = MessageId("message-30"),
        )
        composeTestRule.waitForIdle()

        assertMessageIsCentered(messageId = MessageId("message-30"))
    }

    @Test
    fun pendingScrollMessageId_outsideTheWindow_staysPendingUntilTheGrownWindowBringsItIn() {
        val screenModel = createScreenModel()
        val newerMessages = createMessages(
            count = 50,
            latestMessageId = "message-50",
            latestMessageIncoming = false,
        )
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(messages = newerMessages)
        var consumedCount = 0

        setContent(
            screenModel = screenModel.model,
            pendingScrollMessageId = MessageId("older-5"),
            onPendingScrollMessageIdConsumed = { consumedCount += 1 },
        )
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            screenModel.model.onLoadMessage(messageId = MessageId("older-5"))
        }
        composeTestRule.runOnIdle {
            assertEquals(0, consumedCount)
            screenModel.scaffoldUiStateFlow.value = createPresentUiState(
                messages = createMessages(
                    count = 20,
                    latestMessageId = "older-20",
                    latestMessageIncoming = false,
                    messageIdPrefix = "older",
                ) + newerMessages,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("older-5")))
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(1, consumedCount)
        }
        verify(exactly = 1) {
            screenModel.model.onTargetMessageShown(messageId = MessageId("older-5"))
        }
    }

    /** The window moved around the target holds as many messages as the newest ones did. */
    @Test
    fun pendingScrollMessageId_outsideTheWindow_centersItInTheWindowMovedAroundIt() {
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
            pendingScrollMessageId = MessageId("older-25"),
            onPendingScrollMessageIdConsumed = { consumedCount += 1 },
        )
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            screenModel.scaffoldUiStateFlow.value = createPresentUiState(
                messages = createMessages(
                    count = 50,
                    latestMessageId = "older-50",
                    latestMessageIncoming = false,
                    messageIdPrefix = "older",
                ),
            ).withMessagesState { messagesState ->
                messagesState.copy(windowId = 1, hasNewerMessages = true)
            }
        }
        composeTestRule.waitForIdle()

        assertMessageIsCentered(messageId = MessageId("older-25"))
        composeTestRule.runOnIdle {
            assertEquals(1, consumedCount)
        }
        verify(exactly = 1) {
            screenModel.model.onTargetMessageShown(messageId = MessageId("older-25"))
        }
    }

    @Test
    fun highlightedMessage_flashOver_reportsTheHighlightFinished() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 8,
                latestMessageId = "message-8",
                latestMessageIncoming = false,
            ),
        ).withMessagesState { messagesState ->
            messagesState.copy(highlightedMessageId = MessageId("message-6"))
        }
        composeTestRule.mainClock.autoAdvance = false

        setContent(screenModel = screenModel.model)
        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = MessageHighlightDuration.inWholeMilliseconds / 2,
        )

        verify(exactly = 0) {
            screenModel.model.onMessageHighlightFinished(messageId = any())
        }

        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = MessageHighlightDuration.inWholeMilliseconds,
        )

        verify(exactly = 1) {
            screenModel.model.onMessageHighlightFinished(messageId = MessageId("message-6"))
        }
    }

    @Test
    fun nullPendingScrollMessageId_doesNotInvokeConsumed() {
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
            pendingScrollMessageId = null,
            onPendingScrollMessageIdConsumed = { consumedCount += 1 },
        )

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(0, consumedCount)
        }
    }

    @Test
    fun missingPendingScrollMessageId_consumesOnceWithoutChangingVisibleItem() {
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 50,
                latestMessageId = "message-50",
                latestMessageIncoming = false,
            ),
        )
        screenModel.scaffoldUiStateFlow.value = uiState
        var consumedCount = 0

        setContent(
            screenModel = screenModel.model,
            pendingScrollMessageId = MessageId("missing-message"),
            onPendingScrollMessageIdConsumed = { consumedCount += 1 },
        )

        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertEquals(0, consumedCount)
            screenModel.scaffoldUiStateFlow.value = uiState.withMessagesState { messagesState ->
                messagesState.copy(missingMessageId = MessageId("missing-message"))
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("message-50")))
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(1, consumedCount)
            screenModel.scaffoldUiStateFlow.value = uiState
                .withMessagesState { messagesState ->
                    messagesState.copy(missingMessageId = MessageId("missing-message"))
                }
                .copy(isBlocked = true)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = MessageId("message-50")))
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(1, consumedCount)
        }
        verify(exactly = 0) {
            screenModel.model.onTargetMessageShown(messageId = any())
        }
    }

    private fun assertMessageIsCentered(messageId: MessageId) {
        val listCenter = composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
            .center
            .y
        val messageCenter = composeTestRule
            .onNodeWithTag(conversationMessageItemTestTag(messageId = messageId))
            .fetchSemanticsNode()
            .boundsInRoot
            .center
            .y

        assertEquals(listCenter, messageCenter, CENTER_TOLERANCE_PX)
    }

    private fun ConversationScreenScaffoldUiState.withMessagesState(
        transform: (ConversationMessagesUiState.Present) -> ConversationMessagesUiState.Present,
    ): ConversationScreenScaffoldUiState {
        return copy(messages = transform(messages as ConversationMessagesUiState.Present))
    }

    private companion object {
        /** Halving an odd item height rounds down by up to a pixel. */
        private const val CENTER_TOLERANCE_PX = 1f
    }
}
