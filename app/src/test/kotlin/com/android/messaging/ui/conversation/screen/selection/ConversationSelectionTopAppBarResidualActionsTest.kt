package com.android.messaging.ui.conversation.screen.selection

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageSelectionActionButtonTestTag
import com.android.messaging.ui.conversation.screen.BaseConversationScreenTest
import com.android.messaging.ui.conversation.screen.model.ConversationMessageAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationSelectionTopAppBarResidualActionsTest : BaseConversationScreenTest() {

    @Test
    fun primaryDownloadAndResendActions_areRenderedAndForwardClicks() {
        val screenModel = createScreenModel()
        setSelectionContent(
            screenModel = screenModel,
            actions = listOf(
                ConversationMessageAction.Download,
                ConversationMessageAction.Resend,
            ),
        )

        clickSelectionAction(action = ConversationMessageAction.Download)
        clickSelectionAction(action = ConversationMessageAction.Resend)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageAction.Download,
                )
            }
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageAction.Resend,
                )
            }
        }
    }

    @Test
    fun overflowShareForwardAndDetailsActions_dismissMenuAndForwardClicks() {
        val screenModel = createScreenModel()
        setSelectionContent(
            screenModel = screenModel,
            actions = listOf(
                ConversationMessageAction.Share,
                ConversationMessageAction.Forward,
                ConversationMessageAction.Details,
            ),
        )

        clickOverflowSelectionAction(action = ConversationMessageAction.Share)
        assertOverflowActionHidden(action = ConversationMessageAction.Share)
        clickOverflowSelectionAction(action = ConversationMessageAction.Forward)
        clickOverflowSelectionAction(action = ConversationMessageAction.Details)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageAction.Share,
                )
            }
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageAction.Forward,
                )
            }
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageAction.Details,
                )
            }
        }
    }

    private fun setSelectionContent(
        screenModel: ScreenModelHandle,
        actions: List<ConversationMessageAction>,
    ) {
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = MESSAGE_ID,
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf(MessageId(MESSAGE_ID)),
                availableActions = persistentSetOf(*actions.toTypedArray()),
            ),
        )

        setContent(screenModel = screenModel.model)
    }

    private fun clickSelectionAction(action: ConversationMessageAction) {
        composeTestRule
            .onNodeWithTag(selectionActionTag(action = action))
            .assertIsDisplayed()
            .performClick()
    }

    private fun clickOverflowSelectionAction(action: ConversationMessageAction) {
        composeTestRule
            .onNodeWithTag(CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()
        clickSelectionAction(action = action)
    }

    @Suppress("SameParameterValue")
    private fun assertOverflowActionHidden(action: ConversationMessageAction) {
        composeTestRule
            .onAllNodesWithTag(selectionActionTag(action = action))
            .assertCountEquals(expectedSize = 0)
    }

    private fun selectionActionTag(action: ConversationMessageAction): String {
        return conversationMessageSelectionActionButtonTestTag(action = action.name)
    }

    private companion object {
        private const val MESSAGE_ID = "message-1"
    }
}
