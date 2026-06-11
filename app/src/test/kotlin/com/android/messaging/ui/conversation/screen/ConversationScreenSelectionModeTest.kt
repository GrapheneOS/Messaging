package com.android.messaging.ui.conversation.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_DELETE_MESSAGES_CONFIRM_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_DELETE_MESSAGES_DISMISS_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.conversationMessageSelectionActionButtonTestTag
import com.android.messaging.ui.conversation.screen.model.ConversationMessageDeleteConfirmationUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionAction
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenSelectionModeTest : BaseConversationScreenTest() {

    @Test
    fun singleSelection_showsCopyAndDeleteInTopAppBar_andForwardsActionClicks() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Copy,
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.Copy.name,
                ),
            )
            .assertIsDisplayed()
            .performClick()
        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Copy,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.Delete.name,
                ),
            )
            .assertIsDisplayed()
            .performClick()
        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.Delete,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.Copy.name,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.Delete.name,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun singleSelectionWithSaveAttachment_showsSaveActionInOverflow_andForwardsClick() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = true,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                    ConversationMessageSelectionAction.SaveAttachment,
                ),
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onAllNodesWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.SaveAttachment.name,
                ),
            )
            .assertCountEquals(expectedSize = 0)

        composeTestRule
            .onNodeWithTag(CONVERSATION_SELECTION_OVERFLOW_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.SaveAttachment.name,
                ),
            )
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onMessageSelectionActionClick(
                    action = ConversationMessageSelectionAction.SaveAttachment,
                )
            }
        }
    }

    @Test
    fun multiSelection_showsOnlyDeleteActionInTopAppBar() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1", "message-2"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                ),
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithText(
                targetContext.getString(
                    R.string.conversation_message_selection_title,
                    2,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.Delete.name,
                ),
            )
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithTag(
                conversationMessageSelectionActionButtonTestTag(
                    action = ConversationMessageSelectionAction.Copy.name,
                ),
            )
            .assertCountEquals(expectedSize = 0)
    }

    @Test
    fun deleteConfirmationButtons_forwardToScreenModel() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
                availableActions = persistentSetOf(
                    ConversationMessageSelectionAction.Delete,
                ),
                deleteConfirmation = ConversationMessageDeleteConfirmationUiState(
                    messageIds = persistentSetOf("message-1"),
                ),
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_DELETE_MESSAGES_DISMISS_BUTTON_TEST_TAG)
            .performClick()
        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.dismissDeleteMessageConfirmation()
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_DELETE_MESSAGES_CONFIRM_BUTTON_TEST_TAG)
            .performClick()
        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.confirmDeleteSelectedMessages()
            }
        }
    }
}
