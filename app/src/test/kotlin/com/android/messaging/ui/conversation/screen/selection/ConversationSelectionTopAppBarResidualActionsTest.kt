package com.android.messaging.ui.conversation.screen.selection

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.ui.conversation.CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageSelectionActionButtonTestTag
import com.android.messaging.ui.conversation.screen.BaseConversationScreenTest
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
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
                ConversationMessageSelectionAction.Download,
                ConversationMessageSelectionAction.Resend,
            ),
        )

        clickSelectionAction(action = ConversationMessageSelectionAction.Download)
        clickSelectionAction(action = ConversationMessageSelectionAction.Resend)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Download,
                )
            }
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Resend,
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
                ConversationMessageSelectionAction.Share,
                ConversationMessageSelectionAction.Forward,
                ConversationMessageSelectionAction.Details,
            ),
        )

        clickOverflowSelectionAction(action = ConversationMessageSelectionAction.Share)
        assertOverflowActionHidden(action = ConversationMessageSelectionAction.Share)
        clickOverflowSelectionAction(action = ConversationMessageSelectionAction.Forward)
        clickOverflowSelectionAction(action = ConversationMessageSelectionAction.Details)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Share,
                )
            }
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Forward,
                )
            }
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Details,
                )
            }
        }
    }

    private fun setSelectionContent(
        screenModel: ScreenModelHandle,
        actions: List<ConversationMessageSelectionAction>,
    ) {
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = MESSAGE_ID,
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf(MESSAGE_ID),
                availableActions = persistentSetOf(*actions.toTypedArray()),
            ),
        )

        setContent(screenModel = screenModel.model)
    }

    private fun clickSelectionAction(action: ConversationMessageSelectionAction) {
        composeTestRule
            .onNodeWithTag(selectionActionTag(action = action))
            .assertIsDisplayed()
            .performClick()
    }

    private fun clickOverflowSelectionAction(action: ConversationMessageSelectionAction) {
        composeTestRule
            .onNodeWithTag(CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()
        clickSelectionAction(action = action)
    }

    @Suppress("SameParameterValue")
    private fun assertOverflowActionHidden(action: ConversationMessageSelectionAction) {
        composeTestRule
            .onAllNodesWithTag(selectionActionTag(action = action))
            .assertCountEquals(expectedSize = 0)
    }

    private fun selectionActionTag(action: ConversationMessageSelectionAction): String {
        return conversationMessageSelectionActionButtonTestTag(action = action.name)
    }

    private companion object {
        private const val MESSAGE_ID = "message-1"
    }
}
