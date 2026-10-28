package com.android.messaging.ui.conversationlist.chats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.common.support.CONVERSATION_LIST_SEARCH_RESULTS_TEST_TAG
import com.android.messaging.ui.conversationlist.common.support.conversationListItemTestTag
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListSearchContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListSearchMessageUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListSearchContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val emittedActions = mutableListOf<Action>()

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun results_showTheConversationsUnderTheirHeading() {
        setContent(
            content = ConversationListSearchContentUiState.Results(
                conversations = persistentListOf(
                    previewConversationListItem(
                        conversationId = CONVERSATION_ID,
                        title = TITLE,
                        snippetText = null,
                    ).copy(
                        titleMatchRanges = persistentListOf(0..1),
                        isArchivedBadgeVisible = true,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(CONVERSATION_LIST_SEARCH_RESULTS_TEST_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.conversation_list_search_conversations_header))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(conversationListItemTestTag(CONVERSATION_ID))
            .assertIsDisplayed()
    }

    @Test
    fun result_clicked_opensItsConversation() {
        setContent(
            content = ConversationListSearchContentUiState.Results(
                conversations = persistentListOf(
                    previewConversationListItem(
                        conversationId = CONVERSATION_ID,
                        title = TITLE,
                        snippetText = null,
                    ),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(conversationListItemTestTag(CONVERSATION_ID)).performClick()

        assertEquals(listOf(Action.SearchConversationClicked(CONVERSATION_ID)), emittedActions)
    }

    @Test
    fun messageResults_showUnderTheirHeadingInTheirConversationsRows() {
        setContent(
            content = ConversationListSearchContentUiState.Results(
                conversations = persistentListOf(),
                messages = persistentListOf(groupMessage()),
            ),
        )

        composeTestRule
            .onNodeWithText(string(R.string.conversation_list_search_messages_header))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.conversation_list_search_conversations_header))
            .assertDoesNotExist()
        composeTestRule
            .onNode(hasContentDescription(value = MESSAGE_TEXT, substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun messageResult_clicked_opensItsConversationAtTheMessage() {
        setContent(
            content = ConversationListSearchContentUiState.Results(
                conversations = persistentListOf(),
                messages = persistentListOf(groupMessage()),
            ),
        )

        composeTestRule.onNodeWithTag(conversationListItemTestTag(CONVERSATION_ID)).performClick()

        assertEquals(
            listOf(
                Action.SearchMessageClicked(
                    conversationId = CONVERSATION_ID,
                    messageId = MESSAGE_ID,
                ),
            ),
            emittedActions,
        )
    }

    @Test
    fun noResults_saysSo() {
        setContent(content = ConversationListSearchContentUiState.NoResults)

        composeTestRule
            .onNodeWithText(string(R.string.conversation_list_search_no_results))
            .assertIsDisplayed()
    }

    @Test
    fun idle_showsNothing() {
        setContent(content = ConversationListSearchContentUiState.Idle)

        composeTestRule
            .onNodeWithTag(CONVERSATION_LIST_SEARCH_RESULTS_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(R.string.conversation_list_search_no_results))
            .assertDoesNotExist()
    }

    private fun setContent(content: ConversationListSearchContentUiState) {
        composeTestRule.setContent {
            AppTheme {
                ConversationListSearchContent(
                    content = content,
                    scaffoldContentPadding = PaddingValues(),
                    onAction = { action -> emittedActions += action },
                )
            }
        }
    }

    private fun groupMessage(): ConversationListSearchMessageUiModel {
        return ConversationListSearchMessageUiModel(
            messageId = MESSAGE_ID,
            item = previewConversationListItem(
                conversationId = CONVERSATION_ID,
                title = TITLE,
                snippetText = MESSAGE_TEXT,
                senderName = SENDER_NAME,
                isGroup = true,
            ).copy(snippetMatchRanges = persistentListOf(0..4)),
        )
    }

    private fun string(resId: Int): String {
        return targetContext.getString(resId)
    }

    private companion object {
        private const val TITLE = "Anna"
        private const val SENDER_NAME = "Bob"
        private const val MESSAGE_TEXT = "Lunch at noon"
        private val CONVERSATION_ID = ConversationId("1")
        private val MESSAGE_ID = MessageId("10")
    }
}
