package com.android.messaging.ui.conversationlist.chats

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListSearchTopAppBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun closeButton_clicked_closesSearch() {
        var closeClicks = 0
        composeTestRule.setContent {
            AppTheme {
                ConversationListSearchTopAppBar(
                    searchState = TextFieldState(),
                    onCloseClick = { closeClicks++ },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(string(R.string.conversation_list_search_close))
            .performClick()

        assertEquals(1, closeClicks)
    }

    @Test
    fun clearButton_withText_clearsTheField() {
        val searchState = TextFieldState(initialText = QUERY)
        composeTestRule.setContent {
            AppTheme {
                ConversationListSearchTopAppBar(
                    searchState = searchState,
                    onCloseClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(string(R.string.conversation_list_search_clear))
            .performClick()

        assertEquals("", searchState.text.toString())
        composeTestRule
            .onNodeWithContentDescription(string(R.string.conversation_list_search_clear))
            .assertDoesNotExist()
    }

    @Test
    fun searchButton_onListTopBar_opensSearch() {
        val emittedActions = mutableListOf<Action>()
        composeTestRule.setContent {
            AppTheme {
                ConversationListTopAppBar(
                    hasBlockedParticipants = false,
                    isDebugEnabled = false,
                    onAction = { action -> emittedActions += action },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(string(R.string.conversation_list_search))
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf(Action.SearchClicked), emittedActions)
    }

    private fun string(resId: Int): String {
        return targetContext.getString(resId)
    }

    private companion object {
        private const val QUERY = "anna"
    }
}
