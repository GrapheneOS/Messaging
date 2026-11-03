package com.android.messaging.ui.conversationlist.chats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
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
import com.android.messaging.ui.conversationlist.common.support.conversationListItemTestTag
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListScrollToTopTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val effects = Channel<Effect>(capacity = Channel.UNLIMITED)
    private val handledEffects = mutableListOf<Effect>()

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun dragInterruptingScrollToTop_laterEffectsAreStillHandled() {
        setContent()
        startScrollToTop()
        list().performTouchInput { swipeUp() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(testTag = rowTag(index = 0)).assertDoesNotExist()

        effects.trySend(Effect.OpenDebugOptions)
        composeTestRule.waitForIdle()

        assertEquals(listOf(Effect.OpenDebugOptions), handledEffects)
    }

    private fun startScrollToTop() {
        list().performScrollToIndex(index = START_INDEX)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        effects.trySend(Effect.ScrollToTop)
        repeat(SCROLL_FRAMES_BEFORE_DRAG) { composeTestRule.mainClock.advanceTimeByFrame() }
    }

    private fun setContent() {
        val items = (0 until CONVERSATION_COUNT).map { index ->
            previewConversationListItem(
                conversationId = conversationId(index = index),
                title = "Conversation $index",
                snippetText = "Message $index",
            )
        }.toImmutableList()
        val screenModel = FakeScreenModel(
            effects = effects.receiveAsFlow(),
            uiState = MutableStateFlow(
                State(content = ConversationListContentUiState.Items(items = items)),
            ),
        )
        val navigation = ConversationListNavigationCallbacks(
            onNavigateToConversation = {},
            onNavigateToNewChat = {},
            onNavigateToConversationSettings = {},
            onCloseConversation = {},
            onNavigateToArchivedConversations = {},
            onNavigateToBlockedParticipants = {},
            onNavigateToSettings = {},
        )
        val effectHandler = object : ConversationListEffectHandler {
            override fun handle(effect: Effect) {
                handledEffects += effect
            }
        }
        val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    ConversationListScreen(
                        screenModel = screenModel,
                        effectHandler = effectHandler,
                        navigation = navigation,
                        openedConversationId = null,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun list(): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasScrollToIndexAction())
    }

    private fun rowTag(index: Int): String {
        return conversationListItemTestTag(conversationId = conversationId(index = index))
    }

    private fun conversationId(index: Int): ConversationId {
        return ConversationId("$index")
    }

    private class FakeScreenModel(
        override val effects: Flow<Effect>,
        override val uiState: StateFlow<State>,
    ) : ConversationListScreenModel {
        override val navigationEvents: Flow<NavEvent> = emptyFlow()

        override fun onAction(action: Action) {}
    }

    private companion object {
        const val CONVERSATION_COUNT = 40
        const val START_INDEX = 30
        const val SCROLL_FRAMES_BEFORE_DRAG = 3
    }
}
