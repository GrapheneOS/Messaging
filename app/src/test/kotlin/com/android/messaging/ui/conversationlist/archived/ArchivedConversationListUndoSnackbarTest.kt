package com.android.messaging.ui.conversationlist.archived

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListAction as Action
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListNavEvent as NavEvent
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListUiState as State
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf
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
internal class ArchivedConversationListUndoSnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val effects = Channel<Effect>(capacity = Channel.UNLIMITED)
    private val actions = mutableListOf<Action>()

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun undo_afterUnarchivingOneAtATime_restoresEveryUnarchivedConversation() {
        setContent()

        sendEffect(effect = unarchived(conversationId = FIRST))
        sendEffect(effect = unarchived(conversationId = SECOND))
        composeTestRule.onNodeWithText(text = unarchivedMessage(count = 2)).assertExists()
        composeTestRule.onNodeWithText(text = targetContext.getString(R.string.snack_bar_undo))
            .performClick()
        composeTestRule.waitForIdle()

        val expectedUndo = Action.UnarchiveUndoClicked(conversationIds = listOf(FIRST, SECOND))
        assertEquals(listOf(expectedUndo), snackbarActions())
    }

    @Test
    fun timeout_afterUnarchivingOneAtATime_dismissesEveryUnarchivedConversation() {
        setContent()
        composeTestRule.mainClock.autoAdvance = false

        sendEffect(effect = unarchived(conversationId = FIRST))
        sendEffect(effect = unarchived(conversationId = SECOND))
        composeTestRule.mainClock.advanceTimeBy(milliseconds = PAST_TIMEOUT_MILLIS)

        val expectedDismissal = Action.UnarchiveSnackbarDismissed(
            conversationIds = listOf(FIRST, SECOND),
        )
        assertEquals(listOf(expectedDismissal), snackbarActions())
    }

    private fun unarchived(conversationId: ConversationId): Effect {
        return Effect.ConversationsUnarchived(conversationIds = persistentListOf(conversationId))
    }

    private fun sendEffect(effect: Effect) {
        effects.trySend(effect)
        composeTestRule.waitForIdle()
    }

    private fun unarchivedMessage(count: Int): String {
        return targetContext.getString(R.string.unarchived_toast_message, count)
    }

    private fun snackbarActions(): List<Action> {
        return actions.filterIsInstance<Action.SnackbarAction>()
    }

    private fun setContent() {
        val items = listOf(FIRST, SECOND).map { conversationId ->
            previewConversationListItem(
                conversationId = conversationId,
                title = "Conversation ${conversationId.value}",
                snippetText = "Message ${conversationId.value}",
            )
        }.toImmutableList()
        val screenModel = FakeScreenModel(
            effects = effects.receiveAsFlow(),
            uiState = MutableStateFlow(
                State(content = ConversationListContentUiState.Items(items = items)),
            ),
            onAction = { action -> actions += action },
        )
        val effectHandler = object : ArchivedConversationListEffectHandler {
            override fun handle(effect: Effect) {}
        }
        val lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    ArchivedConversationListScreen(
                        screenModel = screenModel,
                        effectHandler = effectHandler,
                        onNavigateBack = {},
                        onNavigateToConversation = {},
                        onNavigateToConversationSettings = {},
                        onCloseConversation = {},
                        onCloseArchivedList = {},
                        openedConversationId = null,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private class FakeScreenModel(
        override val effects: Flow<Effect>,
        override val uiState: StateFlow<State>,
        private val onAction: (Action) -> Unit,
    ) : ArchivedConversationListScreenModel {
        override val navigationEvents: Flow<NavEvent> = emptyFlow()

        override fun onAction(action: Action) {
            onAction.invoke(action)
        }
    }

    private companion object {
        val FIRST = ConversationId("1")
        val SECOND = ConversationId("2")
        const val PAST_TIMEOUT_MILLIS = 10_000L
    }
}
