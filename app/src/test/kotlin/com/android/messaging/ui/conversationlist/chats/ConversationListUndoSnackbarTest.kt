package com.android.messaging.ui.conversationlist.chats

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
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.chats.model.ConversationListNavEvent as NavEvent
import com.android.messaging.ui.conversationlist.chats.model.ConversationListUiState as State
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
internal class ConversationListUndoSnackbarTest {

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
    fun undo_afterArchivingOneAtATime_restoresEveryArchivedConversation() {
        setContent()

        sendEffect(effect = archived(conversationId = FIRST, isPinned = true))
        sendEffect(effect = archived(conversationId = SECOND, isPinned = false))
        sendEffect(effect = archived(conversationId = THIRD, isPinned = true))
        composeTestRule.onNodeWithText(text = archivedMessage(count = 3)).assertExists()
        composeTestRule.onNodeWithText(text = archivedMessage(count = 1)).assertDoesNotExist()
        undo()

        val expectedUndo = Action.ArchiveUndoClicked(
            conversationIds = persistentListOf(FIRST, SECOND, THIRD),
            pinnedConversationIds = persistentListOf(FIRST, THIRD),
            isArchived = true,
        )
        assertEquals(listOf(expectedUndo), snackbarActions())
    }

    @Test
    fun block_whileArchivePromptIsShown_replacesIt() {
        setContent()

        sendEffect(effect = archived(conversationId = FIRST, isPinned = false))
        sendEffect(
            effect = Effect.ConversationBlocked(
                conversationId = SECOND,
                destination = DESTINATION,
                wasPinned = false,
                success = true,
            ),
        )
        undo()

        val expectedActions = listOf(
            Action.ArchiveSnackbarDismissed(conversationIds = persistentListOf(FIRST)),
            Action.BlockUndoClicked(
                conversationId = SECOND,
                destination = DESTINATION,
                wasPinned = false,
            ),
        )
        assertEquals(expectedActions, snackbarActions())
    }

    private fun archived(conversationId: ConversationId, isPinned: Boolean): Effect {
        return Effect.ArchiveStatusChanged(
            conversationIds = persistentListOf(conversationId),
            pinnedConversationIds = when {
                isPinned -> persistentListOf(conversationId)
                else -> persistentListOf()
            },
            isArchived = true,
        )
    }

    private fun sendEffect(effect: Effect) {
        effects.trySend(effect)
        composeTestRule.waitForIdle()
    }

    private fun undo() {
        composeTestRule.onNodeWithText(text = targetContext.getString(R.string.snack_bar_undo))
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun archivedMessage(count: Int): String {
        return targetContext.getString(R.string.archived_toast_message, count)
    }

    private fun snackbarActions(): List<Action> {
        return actions.filterIsInstance<Action.SnackbarAction>()
    }

    private fun setContent() {
        val items = listOf(FIRST, SECOND, THIRD).map { conversationId ->
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
            override fun handle(effect: Effect) {}
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

    private class FakeScreenModel(
        override val effects: Flow<Effect>,
        override val uiState: StateFlow<State>,
        private val onAction: (Action) -> Unit,
    ) : ConversationListScreenModel {
        override val navigationEvents: Flow<NavEvent> = emptyFlow()

        override fun onAction(action: Action) {
            onAction.invoke(action)
        }
    }

    private companion object {
        val FIRST = ConversationId("1")
        val SECOND = ConversationId("2")
        val THIRD = ConversationId("3")
        const val DESTINATION = "+15550100"
    }
}
