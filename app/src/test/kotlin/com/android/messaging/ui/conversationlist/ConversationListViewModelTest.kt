package com.android.messaging.ui.conversationlist

import app.cash.turbine.test
import com.android.messaging.data.conversationlist.model.ConversationListDraft
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListLatestMessage
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListNotification
import com.android.messaging.data.conversationlist.model.ConversationListParticipant
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
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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

    private val repository = mockk<ConversationListRepository>(relaxUnitFun = true)
    private val uiStateMapper = mockk<ConversationListUiStateMapper>()
    private val selectionDelegate =
        mockk<ConversationListSelectionDelegate>(relaxUnitFun = true)
    private val actionsDelegate = mockk<ConversationListActionsDelegate>(relaxUnitFun = true)
    private val optimisticSnapshotDelegate =
        mockk<ConversationListOptimisticSnapshotDelegate>(relaxUnitFun = true)
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
        every { optimisticSnapshotDelegate.snapshot } returns snapshotFlow
        every { selectionDelegate.selectedIds } returns selectedIdsFlow
        every { actionsDelegate.effects } returns emptyFlow()
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

    private fun snapshotOf(vararg items: ConversationListItem): ConversationListSnapshot {
        return ConversationListSnapshot(
            items = persistentListOf(*items),
            blockedDestinations = persistentSetOf(),
            hasFirstSyncCompleted = true,
        )
    }

    private fun conversationItem(
        conversationId: String,
        isRead: Boolean = true,
    ): ConversationListItem {
        return ConversationListItem(
            conversationId = conversationId,
            title = "Title $conversationId",
            icon = null,
            subject = null,
            isArchived = false,
            isPinned = false,
            participant = ConversationListParticipant(
                contactId = -1L,
                lookupKey = null,
                otherNormalizedDestination = "+1555000$conversationId",
                isGroup = false,
                isEnterprise = false,
            ),
            latestMessage = ConversationListLatestMessage(
                isRead = isRead,
                timestamp = 1_000L,
                snippetText = "Snippet $conversationId",
                previewUri = null,
                previewContentType = null,
                status = ConversationListMessageStatus.Normal,
                isIncoming = true,
                senderName = null,
            ),
            draft = ConversationListDraft(
                isVisible = false,
                snippetText = null,
                previewUri = null,
                previewContentType = null,
                subject = null,
            ),
            notification = ConversationListNotification(isEnabled = true),
        )
    }
}
