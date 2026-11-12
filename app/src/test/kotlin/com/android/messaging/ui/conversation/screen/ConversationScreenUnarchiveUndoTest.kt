package com.android.messaging.ui.conversation.screen

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.screen.model.ConversationScreenNavEvent
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenUnarchiveUndoTest : BaseConversationScreenTest() {

    @Test
    fun closeAfterUnarchive_closesAndLeavesUndoToTheArchivedList() {
        val navigationEvents = MutableSharedFlow<ConversationScreenNavEvent>(
            extraBufferCapacity = 1,
        )
        val unarchivedConversationIds = MutableSharedFlow<ConversationId>(
            extraBufferCapacity = 1,
        )
        val screenModel = createScreenModel().model
        every { screenModel.navigationEvents } returns navigationEvents
        every { screenModel.unarchivedConversationIds } returns unarchivedConversationIds
        var closeCount = 0

        setContent(
            screenModel = screenModel,
            offersUnarchiveUndo = false,
            onCloseConversation = { closeCount += 1 },
        )
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            navigationEvents.subscriptionCount.value == 1
        }

        navigationEvents.tryEmit(ConversationScreenNavEvent.CloseAfterUnarchive)

        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            closeCount == 1
        }
        assertEquals(0, unarchivedConversationIds.subscriptionCount.value)
    }

    @Test
    fun closeAfterUnarchive_staysOpenAndOffersUndoWithoutAnArchivedList() {
        val navigationEvents = MutableSharedFlow<ConversationScreenNavEvent>(
            extraBufferCapacity = 1,
        )
        val unarchivedConversationIds = MutableSharedFlow<ConversationId>(
            extraBufferCapacity = 1,
        )
        val screenModel = createScreenModel().model
        every { screenModel.navigationEvents } returns navigationEvents
        every { screenModel.unarchivedConversationIds } returns unarchivedConversationIds
        var closeCount = 0

        setContent(
            screenModel = screenModel,
            offersUnarchiveUndo = true,
            onCloseConversation = { closeCount += 1 },
        )
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            navigationEvents.subscriptionCount.value == 1 &&
                unarchivedConversationIds.subscriptionCount.value == 1
        }

        navigationEvents.tryEmit(ConversationScreenNavEvent.CloseAfterUnarchive)
        unarchivedConversationIds.tryEmit(CONVERSATION_ID)

        val activity = composeTestRule.activity
        composeTestRule
            .onNodeWithText(activity.getString(R.string.unarchived_toast_message, 1))
            .assertExists()
        composeTestRule
            .onNodeWithText(activity.getString(R.string.snack_bar_undo))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onUndoUnarchiveClick(conversationId = CONVERSATION_ID)
            }
            assertEquals(0, closeCount)
        }
    }
}
