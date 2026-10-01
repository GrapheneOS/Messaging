package com.android.messaging.ui.conversationlist.chats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.event.ConversationArchiveEventsImpl
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.conversationlist.chats.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.chats.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.chats.model.ConversationListUiState as State
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.delegate.ConversationListOptimisticSnapshotDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.snapshotOfIds
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * While the newest conversation is visible, incoming messages are stored as seen and never
 * notified, so the list must only claim it while the screen is actually in front of the user
 * (GrapheneOS/Messaging#391)
 */
@RunWith(RobolectricTestRunner::class)
internal class ConversationListNewestConversationVisibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = mockk<ConversationListRepository>(relaxed = true)
    private val visibilityUpdates = mutableListOf<Boolean>()

    private lateinit var lifecycleOwner: TestLifecycleOwner
    private var isScreenShown by mutableStateOf(true)

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
        every { repository.setNewestConversationVisible(any()) } answers {
            visibilityUpdates += firstArg<Boolean>()
        }
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun resumedAtTop_newestConversationIsVisible() {
        setContent()

        assertTrue(isNewestConversationVisible())
    }

    @Test
    fun pausedAfterScrollingBackToTop_newestConversationIsNotVisible() {
        setContent()
        scrollAwayAndBackToTop()
        assertTrue(isNewestConversationVisible())

        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.STARTED)
        }
        composeTestRule.waitForIdle()

        assertFalse(isNewestConversationVisible())
    }

    @Test
    fun leftCompositionAfterScrollingBackToTop_newestConversationIsNotVisible() {
        setContent()
        scrollAwayAndBackToTop()
        assertTrue(isNewestConversationVisible())

        composeTestRule.runOnIdle {
            isScreenShown = false
        }
        composeTestRule.waitForIdle()

        assertFalse(isNewestConversationVisible())
    }

    private fun setContent() {
        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        }

        val viewModel = createViewModel()
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

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AppTheme {
                    if (isScreenShown) {
                        ConversationListScreen(
                            screenModel = viewModel,
                            effectHandler = effectHandler,
                            navigation = navigation,
                            openedConversationId = null,
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun scrollAwayAndBackToTop() {
        val list = composeTestRule.onNode(hasScrollToIndexAction())

        list.performScrollToIndex(index = CONVERSATION_COUNT - 1)
        composeTestRule.waitForIdle()
        list.performScrollToIndex(index = 0)
        composeTestRule.waitForIdle()
    }

    /** Mirrors the process-wide DataModel flag, which starts out false. */
    private fun isNewestConversationVisible(): Boolean {
        return visibilityUpdates.lastOrNull() ?: false
    }

    private fun createViewModel(): ConversationListViewModel {
        val items = (1..CONVERSATION_COUNT).map { index ->
            previewConversationListItem(
                conversationId = ConversationId("$index"),
                title = "Conversation $index",
                snippetText = "Message $index",
            )
        }.toImmutableList()

        val uiStateMapper = mockk<ConversationListUiStateMapper>()
        every { uiStateMapper.map(any(), any(), any(), any(), any()) } answers {
            State(
                content = ConversationListContentUiState.Items(items = items),
                isScrollToTopVisible = arg(3),
            )
        }

        val optimisticSnapshotDelegate =
            mockk<ConversationListOptimisticSnapshotDelegate>(relaxed = true)
        every { optimisticSnapshotDelegate.snapshot } returns MutableStateFlow(snapshotOfIds("1"))

        val selectionDelegate = mockk<ConversationListSelectionDelegate>(relaxed = true)
        every { selectionDelegate.selectedIds } returns MutableStateFlow(persistentListOf())

        return ConversationListViewModel(
            repository = repository,
            uiStateMapper = uiStateMapper,
            selectionDelegate = selectionDelegate,
            actionsDelegate = mockk(relaxed = true),
            optimisticSnapshotDelegate = optimisticSnapshotDelegate,
            resolveContactAction = mockk(relaxed = true),
            debugFeaturesProvider = mockk(relaxed = true),
            conversationArchiveEvents = ConversationArchiveEventsImpl(),
            defaultDispatcher = Dispatchers.Main,
            mainDispatcher = Dispatchers.Main,
        )
    }

    private companion object {
        const val CONVERSATION_COUNT = 40
    }
}
