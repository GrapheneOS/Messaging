package com.android.messaging.ui.conversationlist.chats

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.conversationlist.chats.mapper.ConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.ConversationListEffect as Effect
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.delegate.ConversationListActionsDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListOptimisticSnapshotDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.snapshotOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val selectedIdsFlow = MutableStateFlow<ImmutableList<ConversationId>>(
        persistentListOf(),
    )

    @Test
    fun init_bindsDelegates() {
        createViewModel()

        verify(exactly = 1) { optimisticSnapshotDelegate.bind(any(), any()) }
        verify(exactly = 1) { selectionDelegate.bind(any(), snapshotFlow) }
    }

    @Test
    fun archiveClicked_archivesOptimisticallyPersistsAndEmitsStatusEffect() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            selectedIdsFlow.value = persistentListOf(ConversationId("a"))
            snapshotFlow.value = snapshotOf(conversationItem(ConversationId("a")))

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.ArchiveClicked)

                assertThat(awaitItem()).isEqualTo(
                    Effect.ArchiveStatusChanged(
                        conversationIds = persistentListOf(ConversationId("a")),
                        isArchived = true,
                    )
                )
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle()

            verify { optimisticSnapshotDelegate.remove(listOf(ConversationId("a"))) }
            coVerify { actionsDelegate.setArchived(listOf(ConversationId("a")), isArchived = true) }
            verify { selectionDelegate.clear() }
        }
    }

    @Test
    fun swipeToggleRead_marksUnreadConversationReadOptimisticallyAndPersists() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            snapshotFlow.value = snapshotOf(conversationItem(ConversationId("a"), isRead = false))

            val viewModel = createViewModel()
            viewModel.onAction(Action.ConversationSwipedToToggleRead(ConversationId("a")))
            advanceUntilIdle()

            verify {
                optimisticSnapshotDelegate.markRead(listOf(ConversationId("a")), isRead = true)
            }
            coVerify { actionsDelegate.setRead(listOf(ConversationId("a")), isRead = true) }
        }
    }

    @Test
    fun pinClicked_emitsPrepareAnimationEffectForSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            selectedIdsFlow.value = persistentListOf(ConversationId("a"))
            snapshotFlow.value = snapshotOf(conversationItem(ConversationId("a")))

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.PinClicked)

                assertThat(awaitItem()).isEqualTo(
                    Effect.PreparePinAnimation(
                        conversationIds = persistentListOf(ConversationId("a")),
                        isPinned = true,
                    )
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun pinAnimationPrepared_commitsPinChangeAndClearsSelection() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAction(
                Action.PinAnimationPrepared(
                    conversationIds = persistentListOf(ConversationId("a")),
                    isPinned = true,
                ),
            )
            advanceUntilIdle()

            verify { optimisticSnapshotDelegate.pin(listOf(ConversationId("a")), isPinned = true) }
            coVerify { actionsDelegate.setPinned(listOf(ConversationId("a")), isPinned = true) }
            verify { selectionDelegate.clear() }
        }
    }

    @Test
    fun conversationClicked_withoutSelection_emitsOpenConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            snapshotFlow.value = snapshotOf(conversationItem(ConversationId("a")))

            val viewModel = createViewModel()
            viewModel.effects.test {
                viewModel.onAction(Action.ConversationClicked(ConversationId("a")))

                assertThat(awaitItem()).isEqualTo(Effect.OpenConversation(ConversationId("a")))
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
                conversationIds = persistentListOf(ConversationId("a"), ConversationId("b")),
            ),
        )

        verify {
            optimisticSnapshotDelegate.discardRemoval(
                listOf(ConversationId("a"), ConversationId("b")),
            )
        }
    }

    @Test
    fun archiveUndoClicked_restoresItemsAndPersistsUnarchivedState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAction(
                Action.ArchiveUndoClicked(
                    conversationIds = persistentListOf(ConversationId("a")),
                    isArchived = true,
                ),
            )
            advanceUntilIdle()

            verify { optimisticSnapshotDelegate.restore(listOf(ConversationId("a"))) }
            coVerify {
                actionsDelegate.setArchived(listOf(ConversationId("a")), isArchived = false)
            }
        }
    }

    @Test
    fun unarchiveUndoClicked_archivesItemsAndPersistsArchivedState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onAction(
                Action.ArchiveUndoClicked(
                    conversationIds = persistentListOf(ConversationId("a")),
                    isArchived = false,
                ),
            )
            advanceUntilIdle()

            verify { optimisticSnapshotDelegate.remove(listOf(ConversationId("a"))) }
            coVerify { actionsDelegate.setArchived(listOf(ConversationId("a")), isArchived = true) }
        }
    }

    private fun createViewModel(): ConversationListViewModel {
        every { repository.refresh() } just runs
        every { repository.setNewestConversationVisible(any()) } just runs

        every { optimisticSnapshotDelegate.snapshot } returns snapshotFlow
        every { optimisticSnapshotDelegate.bind(any(), any()) } just runs
        every { optimisticSnapshotDelegate.remove(any()) } just runs
        every { optimisticSnapshotDelegate.discardRemoval(any()) } just runs
        every { optimisticSnapshotDelegate.restore(any()) } just runs
        every { optimisticSnapshotDelegate.markRead(any(), any()) } just runs
        every { optimisticSnapshotDelegate.pin(any(), any()) } just runs

        every { selectionDelegate.selectedIds } returns selectedIdsFlow
        every { selectionDelegate.bind(any(), any()) } just runs
        every { selectionDelegate.toggle(any()) } just runs
        every { selectionDelegate.clear() } just runs

        coEvery { actionsDelegate.setArchived(any(), any()) } just runs
        coEvery { actionsDelegate.setPinned(any(), any()) } just runs
        coEvery { actionsDelegate.setRead(any(), any()) } just runs
        every { actionsDelegate.snooze(any(), any()) } just runs
        every { actionsDelegate.unsnooze(any()) } just runs
        every { actionsDelegate.delete(any()) } just runs
        coEvery { actionsDelegate.block(any(), any()) } returns false
        coEvery { actionsDelegate.unblock(any(), any()) } just runs

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
