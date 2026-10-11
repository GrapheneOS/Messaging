package com.android.messaging.ui.search

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.data.search.repository.MessageSearchRepository
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = mockk<MessageSearchRepository>()
    private lateinit var lifecycleOwner: TestLifecycleOwner

    private var openedConversationId: ConversationId? = null

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun results_areShownAndClickingOneOpensItsConversation() {
        every { repository.search("needle") } returns flowOf(
            listOf(result("find this needle")).toImmutableList(),
        )

        setContent()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("needle")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("find this needle")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("find this needle").performClick()

        assertEquals(ConversationId("c1"), openedConversationId)
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

    @Test
    fun blankQuery_doesNotShowTheNoResultsPlaceholder() {
        setContent()

        composeTestRule.onNodeWithText("No results").assertDoesNotExist()
    }

    private fun setContent() {
        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        }

        val viewModel = SearchViewModel(repository, mockk(relaxed = true))

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    SearchScreen(
                        screenModel = viewModel,
                        onNavigateBack = {},
                        onNavigateToConversation = { openedConversationId = it },
                    )
                }
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
