package com.android.messaging.ui.conversationlist

import app.cash.turbine.test
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversationlist.delegate.ConversationListActionsDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListOptimisticSnapshotDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.model.ConversationListEffect as Effect
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<ConversationListRepository>()
    private val uiStateMapper = mockk<ConversationListUiStateMapper>()
    private val selectionDelegate = mockk<ConversationListSelectionDelegate>()
    private val actionsDelegate = mockk<ConversationListActionsDelegate>()
    private val optimisticSnapshotDelegate = mockk<ConversationListOptimisticSnapshotDelegate>()
    private val debugFeaturesProvider = mockk<DebugFeaturesProvider>()

    private val snapshotFlow = MutableStateFlow<ConversationListSnapshot?>(null)
    private val selectedIdsFlow = MutableStateFlow<ImmutableList<String>>(persistentListOf())

    @Test
    fun init_bindsDelegates() {
        createViewModel()

        verify(exactly = 1) { optimisticSnapshotDelegate.bind(any()) }
        verify(exactly = 1) { selectionDelegate.bind(any(), snapshotFlow) }
        verify(exactly = 1) { actionsDelegate.bind(any()) }
    }

    @Test
    fun archiveClicked_archivesOptimisticallyAndPersistsWithSnackbar() {
        selectedIdsFlow.value = persistentListOf("a")
        snapshotFlow.value = snapshotOf(conversationItem("a"))

        val viewModel = createViewModel()
        viewModel.onAction(Action.ArchiveClicked)

        verify { optimisticSnapshotDelegate.archive(listOf("a")) }
        verify {
            actionsDelegate.setArchived(
                conversationIds = listOf("a"),
                isArchived = true,
                shouldShowSnackbar = true,
            )
        }
        verify { selectionDelegate.clear() }
    }

    @Test
    fun swipeToggleRead_marksUnreadConversationReadOptimisticallyAndPersists() {
        snapshotFlow.value = snapshotOf(conversationItem("a", isRead = false))

        val viewModel = createViewModel()
        viewModel.onAction(Action.ConversationSwipedToToggleRead("a"))

        verify { optimisticSnapshotDelegate.markRead(listOf("a"), isRead = true) }
        verify { actionsDelegate.setRead(listOf("a"), isRead = true) }
    }

    @Test
    fun pinClicked_emitsPrepareAnimationEffectForSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            selectedIdsFlow.value = persistentListOf("a")
            snapshotFlow.value = snapshotOf(conversationItem("a"))

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.PinClicked)

                assertEquals(
                    Effect.PreparePinAnimation(
                        conversationIds = persistentListOf("a"),
                        isPinned = true,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun pinAnimationPrepared_commitsPinChangeAndClearsSelection() {
        val viewModel = createViewModel()
        viewModel.onAction(
            Action.PinAnimationPrepared(
                conversationIds = persistentListOf("a"),
                isPinned = true,
            ),
        )

        verify { optimisticSnapshotDelegate.pin(listOf("a"), isPinned = true) }
        verify { actionsDelegate.setPinned(listOf("a"), isPinned = true) }
        verify { selectionDelegate.clear() }
    }

    @Test
    fun conversationClicked_withoutSelection_emitsOpenConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            snapshotFlow.value = snapshotOf(conversationItem("a"))

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.ConversationClicked("a"))

                assertEquals(Effect.OpenConversation("a"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun startChatClicked_emitsStartChatEffect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.StartChatClicked)

                assertEquals(Effect.StartChat, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun archiveSnackbarDismissed_discardsArchivedItems() {
        val viewModel = createViewModel()
        viewModel.onAction(
            Action.ArchiveSnackbarDismissed(
                conversationIds = persistentListOf("a", "b"),
            ),
        )

        verify { optimisticSnapshotDelegate.discardArchived(listOf("a", "b")) }
    }

    @Test
    fun archiveUndoClicked_restoresItemsAndPersistsUnarchivedState() {
        val viewModel = createViewModel()
        viewModel.onAction(
            Action.ArchiveUndoClicked(
                conversationIds = persistentListOf("a"),
                isArchived = true,
            ),
        )

        verify { optimisticSnapshotDelegate.restoreArchived(listOf("a")) }
        verify {
            actionsDelegate.setArchived(
                conversationIds = listOf("a"),
                isArchived = false,
                shouldShowSnackbar = false,
            )
        }
    }

    @Test
    fun unarchiveUndoClicked_archivesItemsAndPersistsArchivedState() {
        val viewModel = createViewModel()
        viewModel.onAction(
            Action.ArchiveUndoClicked(
                conversationIds = persistentListOf("a"),
                isArchived = false,
            ),
        )

        verify { optimisticSnapshotDelegate.archive(listOf("a")) }
        verify {
            actionsDelegate.setArchived(
                conversationIds = listOf("a"),
                isArchived = true,
                shouldShowSnackbar = false,
            )
        }
    }

    private fun createViewModel(): ConversationListViewModel {
        every { repository.refresh() } just runs
        every { repository.setNewestConversationVisible(any()) } just runs

        every { optimisticSnapshotDelegate.snapshot } returns snapshotFlow
        every { optimisticSnapshotDelegate.bind(any()) } just runs
        every { optimisticSnapshotDelegate.archive(any()) } just runs
        every { optimisticSnapshotDelegate.discardArchived(any()) } just runs
        every { optimisticSnapshotDelegate.restoreArchived(any()) } just runs
        every { optimisticSnapshotDelegate.markRead(any(), any()) } just runs
        every { optimisticSnapshotDelegate.pin(any(), any()) } just runs

        every { selectionDelegate.selectedIds } returns selectedIdsFlow
        every { selectionDelegate.bind(any(), any()) } just runs
        every { selectionDelegate.toggle(any()) } just runs
        every { selectionDelegate.clear() } just runs

        every { actionsDelegate.effects } returns emptyFlow()
        every { actionsDelegate.bind(any()) } just runs
        every { actionsDelegate.setArchived(any(), any(), any()) } just runs
        every { actionsDelegate.setPinned(any(), any()) } just runs
        every { actionsDelegate.setRead(any(), any()) } just runs
        every { actionsDelegate.snooze(any(), any()) } just runs
        every { actionsDelegate.unsnooze(any()) } just runs
        every { actionsDelegate.delete(any()) } just runs
        every { actionsDelegate.block(any(), any()) } just runs
        every { actionsDelegate.unblock(any(), any()) } just runs

        every { debugFeaturesProvider.isEnabled() } returns false

        return ConversationListViewModel(
            repository = repository,
            uiStateMapper = uiStateMapper,
            selectionDelegate = selectionDelegate,
            actionsDelegate = actionsDelegate,
            optimisticSnapshotDelegate = optimisticSnapshotDelegate,
            debugFeaturesProvider = debugFeaturesProvider,
        )
    }
}
