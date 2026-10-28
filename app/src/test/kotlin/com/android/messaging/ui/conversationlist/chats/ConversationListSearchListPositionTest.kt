package com.android.messaging.ui.conversationlist.chats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.chats.model.ConversationListNavEvent as NavEvent
import com.android.messaging.ui.conversationlist.chats.model.ConversationListUiState as State
import com.android.messaging.ui.conversationlist.common.support.CONVERSATION_LIST_TEST_TAG
import com.android.messaging.ui.conversationlist.common.support.conversationListItemTestTag
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel as Model
import com.android.messaging.ui.conversationlist.model.ConversationListSearchUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListSearchListPositionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val uiState = MutableStateFlow(
        State(
            content = ConversationListContentUiState.Items(
                items = rows(0 until ROW_COUNT).toImmutableList(),
            ),
        ),
    )

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun searchClosed_listAtTop_showsTheConversationAddedDuringSearch() {
        setContent()

        searchWhileRowsChangeTo(rows = rows(listOf(ROW_COUNT)) + rows(0 until ROW_COUNT))

        assertRowIsFullyInList(index = ROW_COUNT)
    }

    @Test
    fun searchClosed_listAtTop_showsTheConversationMovedUpDuringSearch() {
        setContent()

        searchWhileRowsChangeTo(
            rows = rows(listOf(MOVED_ROW)) + rows((0 until ROW_COUNT).filter { it != MOVED_ROW }),
        )

        assertRowIsFullyInList(index = MOVED_ROW)
    }

    @Test
    fun searchClosed_listScrolledDown_staysOnTheSameConversations() {
        setContent()
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(index = MOVED_ROW)
        composeTestRule.waitForIdle()

        searchWhileRowsChangeTo(rows = rows(listOf(ROW_COUNT)) + rows(0 until ROW_COUNT))

        composeTestRule.onNodeWithTag(rowTag(index = MOVED_ROW)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(rowTag(index = ROW_COUNT)).assertDoesNotExist()
    }

    private fun assertRowIsFullyInList(index: Int) {
        val listBounds = composeTestRule
            .onNodeWithTag(CONVERSATION_LIST_TEST_TAG)
            .getUnclippedBoundsInRoot()
        val rowBounds = composeTestRule
            .onNodeWithTag(rowTag(index = index))
            .getUnclippedBoundsInRoot()

        assertTrue(rowBounds.top >= listBounds.top)
    }

    private fun searchWhileRowsChangeTo(rows: List<Model>) {
        setState {
            copy(search = ConversationListSearchUiState(isActive = true))
        }
        setState {
            copy(content = ConversationListContentUiState.Items(items = rows.toImmutableList()))
        }
        setState {
            copy(search = ConversationListSearchUiState())
        }
    }

    private fun setState(update: State.() -> State) {
        composeTestRule.runOnIdle {
            uiState.value = uiState.value.update()
        }
        composeTestRule.waitForIdle()
    }

    private fun setContent() {
        val navigation = ConversationListNavigationCallbacks(
            onNavigateToConversation = {},
            onNavigateToMessage = { _, _ -> },
            onNavigateToNewChat = {},
            onNavigateToConversationSettings = {},
            onCloseConversation = {},
            onNavigateToArchivedConversations = {},
            onNavigateToBlockedParticipants = {},
            onNavigateToSettings = {},
        )
        val effectHandler = object : ConversationListEffectHandler {
            override fun handle(effect: Effect) {}
        }
        val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    ConversationListScreen(
                        screenModel = FakeScreenModel(),
                        effectHandler = effectHandler,
                        navigation = navigation,
                        openedConversationId = null,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun rows(indices: Iterable<Int>): List<Model> {
        return indices.map { index ->
            previewConversationListItem(
                conversationId = conversationId(index = index),
                title = "Conversation $index",
                snippetText = "Message $index",
            )
        }
    }

    private fun rowTag(index: Int): String {
        return conversationListItemTestTag(conversationId = conversationId(index = index))
    }

    private fun conversationId(index: Int): ConversationId {
        return ConversationId("$index")
    }

    private inner class FakeScreenModel : ConversationListScreenModel {
        override val effects: Flow<Effect> = emptyFlow()
        override val navigationEvents: Flow<NavEvent> = emptyFlow()
        override val uiState = this@ConversationListSearchListPositionTest.uiState

        override fun onAction(action: Action) {}
    }

    private companion object {
        const val ROW_COUNT = 40
        const val MOVED_ROW = 30
    }
}
