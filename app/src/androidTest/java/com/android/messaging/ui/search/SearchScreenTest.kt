package com.android.messaging.ui.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.data.search.repository.MessageSearchRepository
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

internal class SearchScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val repository = mockk<MessageSearchRepository>()

    @Test
    fun results_areShownAndClickingOneOpensItsConversation() {
        every { repository.search("needle") } returns flowOf(
            listOf(result("find this needle")).toImmutableList(),
        )

        var openedConversationId: ConversationId? = null
        setContent(onNavigateToConversation = { openedConversationId = it })

        composeTestRule.onNode(hasSetTextAction()).performTextInput("needle")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("find this needle")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("find this needle").performClick()

        composeTestRule.runOnIdle {
            assertEquals(ConversationId("c1"), openedConversationId)
        }
    }

    @Test
    fun emptyResults_showsNoResultsMessage() {
        every { repository.search("nothing") } returns flowOf(
            emptyList<MessageSearchResult>().toImmutableList(),
        )

        setContent()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("nothing")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("No results")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun setContent(
        onNavigateToConversation: (ConversationId) -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme {
                SearchScreen(
                    screenModel = SearchViewModel(repository),
                    onNavigateBack = {},
                    onNavigateToConversation = onNavigateToConversation,
                )
            }
        }
    }

    private fun result(snippet: String): MessageSearchResult {
        return MessageSearchResult(
            messageId = MessageId("m1"),
            conversationId = ConversationId("c1"),
            snippet = snippet,
            timestamp = 1_700_000_000_000L,
            conversationName = "Conversation one",
            senderName = "Sender one",
        )
    }
}
