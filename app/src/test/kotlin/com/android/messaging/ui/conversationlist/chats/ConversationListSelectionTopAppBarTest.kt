package com.android.messaging.ui.conversationlist.chats

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.SelectionActionsUiState
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The read toggle is the one whose label and action have to move together: a selection that is not
 * entirely read must offer *mark as read*, and tapping that must not mark the selection unread.
 */
@RunWith(RobolectricTestRunner::class)
internal class ConversationListSelectionTopAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val emittedActions = mutableListOf<Action>()

    @Test
    fun aSelectionThatIsNotEntirelyReadOffersToMarkItRead() {
        setSelectionContent(allSelectedAreRead = false)

        openOverflowMenu()
        composeTestRule.onNodeWithText(string(R.string.mark_as_read)).performClick()

        assertEquals(listOf(Action.MarkReadClicked), emittedActions)
    }

    @Test
    fun anEntirelyReadSelectionOffersToMarkItUnread() {
        setSelectionContent(allSelectedAreRead = true)

        openOverflowMenu()
        composeTestRule.onNodeWithText(string(R.string.mark_as_unread)).performClick()

        assertEquals(listOf(Action.MarkUnreadClicked), emittedActions)
    }

    private fun setSelectionContent(allSelectedAreRead: Boolean) {
        composeTestRule.setContent {
            AppTheme {
                ConversationListSelectionTopAppBar(
                    selectedCount = 2,
                    actions = SelectionActionsUiState(allSelectedAreRead = allSelectedAreRead),
                    onAction = { action -> emittedActions += action },
                    onDeleteClick = {},
                    onSnoozeClick = {},
                )
            }
        }
    }

    private fun openOverflowMenu() {
        composeTestRule
            .onNodeWithContentDescription(string(R.string.more_options))
            .performClick()
    }

    private fun string(resId: Int): String {
        return targetContext.getString(resId)
    }
}
