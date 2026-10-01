package com.android.messaging.ui.conversation.screen

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.screen.model.ConversationScreenNavEvent
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenArchiveUndoTest : BaseConversationScreenTest() {

    @Test
    fun closeAfterArchive_closesWhenTheHostHasAConversationList() {
        val navigationEvents = MutableSharedFlow<ConversationScreenNavEvent>(
            extraBufferCapacity = 1,
        )
        val screenModel = createScreenModel().model
        every { screenModel.navigationEvents } returns navigationEvents
        var closeCount = 0

        setContent(
            screenModel = screenModel,
            offersArchiveUndo = false,
            onCloseConversation = { closeCount += 1 },
        )
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            navigationEvents.subscriptionCount.value == 1
        }

        navigationEvents.tryEmit(ConversationScreenNavEvent.CloseAfterArchive)

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            closeCount == 1
        }
    }

    @Test
    fun closeAfterArchive_staysOpenAndOffersUndoWithoutAConversationList() {
        val navigationEvents = MutableSharedFlow<ConversationScreenNavEvent>(
            extraBufferCapacity = 1,
        )
        val archivedConversationIds = MutableSharedFlow<ConversationId>(extraBufferCapacity = 1)
        val screenModel = createScreenModel().model
        every { screenModel.navigationEvents } returns navigationEvents
        every { screenModel.archivedConversationIds } returns archivedConversationIds
        var closeCount = 0

        setContent(
            screenModel = screenModel,
            offersArchiveUndo = true,
            onCloseConversation = { closeCount += 1 },
        )
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            navigationEvents.subscriptionCount.value == 1 &&
                archivedConversationIds.subscriptionCount.value == 1
        }

        navigationEvents.tryEmit(ConversationScreenNavEvent.CloseAfterArchive)
        archivedConversationIds.tryEmit(CONVERSATION_ID)

        val activity = composeTestRule.activity
        composeTestRule
            .onNodeWithText(activity.getString(R.string.archived_toast_message, 1))
            .assertExists()
        composeTestRule
            .onNodeWithText(activity.getString(R.string.snack_bar_undo))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onUndoArchiveClick(conversationId = CONVERSATION_ID)
            }
            assertEquals(0, closeCount)
        }
    }

    @Test
    fun archivedBeforeComposition_undoSurvivesTheInitialScrollToLatest() {
        val handle = createScreenModel()
        val screenModel = handle.model
        every { screenModel.archivedConversationIds } returns flowOf(CONVERSATION_ID)
        handle.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 3,
                latestMessageId = "message-3",
                latestMessageIncoming = false,
            ),
        )

        setContent(
            screenModel = screenModel,
            offersArchiveUndo = true,
        )

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.archived_toast_message, 1))
            .assertExists()
    }

    @Test
    fun archiveWhileTheNewMessageNoticeIsShown_replacesItWithUndo() {
        val archivedConversationIds = MutableSharedFlow<ConversationId>(extraBufferCapacity = 1)
        val handle = createScreenModel()
        val screenModel = handle.model
        every { screenModel.archivedConversationIds } returns archivedConversationIds
        handle.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 30,
                latestMessageId = "message-30",
                latestMessageIncoming = false,
            ),
        )
        setContent(
            screenModel = screenModel,
            offersArchiveUndo = true,
        )
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        handle.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 31,
                latestMessageId = "message-31",
                latestMessageIncoming = true,
            ),
        )
        val activity = composeTestRule.activity
        composeTestRule
            .onNodeWithText(activity.getString(R.string.in_conversation_notify_new_message_text))
            .assertExists()
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            archivedConversationIds.subscriptionCount.value == 1
        }

        archivedConversationIds.tryEmit(CONVERSATION_ID)

        composeTestRule
            .onNodeWithText(activity.getString(R.string.archived_toast_message, 1))
            .assertExists()
    }
}
