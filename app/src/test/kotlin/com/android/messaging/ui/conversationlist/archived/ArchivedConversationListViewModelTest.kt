package com.android.messaging.ui.conversationlist.archived

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.domain.conversation.usecase.participant.model.ResolveContactActionResult
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversationlist.archived.mapper.ArchivedConversationListUiStateMapper
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListAction as Action
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListUiState as State
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.delegate.ConversationListOptimisticSnapshotDelegate
import com.android.messaging.ui.conversationlist.delegate.ConversationListSelectionDelegate
import com.android.messaging.ui.conversationlist.snapshotOf
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ArchivedConversationListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val uiStateMapper = mockk<ArchivedConversationListUiStateMapper>()
    private val optimisticSnapshotDelegate =
        mockk<ConversationListOptimisticSnapshotDelegate>(relaxed = true)
    private val selectionDelegate = mockk<ConversationListSelectionDelegate>(relaxed = true)

    private val snapshotFlow = MutableStateFlow<ConversationListSnapshot?>(
        snapshotOf(conversationItem(conversationId = OPENED_ID, isArchived = true)),
    )
    private val selectedIdsFlow = MutableStateFlow<ImmutableList<ConversationId>>(
        persistentListOf(),
    )

    @Test
    fun uiState_noOpenedConversation_mapsWithoutOpenedConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.uiState.test {
                advanceUntilIdle()

                verify { mapWithOpenedConversation(openedConversationId = null) }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun openedConversationChanged_toConversation_remapsWithOpenedConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.uiState.test {
                viewModel.onAction(Action.OpenedConversationChanged(OPENED_ID))
                advanceUntilIdle()

                verify { mapWithOpenedConversation(openedConversationId = OPENED_ID) }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun openedConversationChanged_toNull_remapsWithoutOpenedConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.uiState.test {
                viewModel.onAction(Action.OpenedConversationChanged(OPENED_ID))
                advanceUntilIdle()

                viewModel.onAction(Action.OpenedConversationChanged(null))
                advanceUntilIdle()

                verifyOrder {
                    mapWithOpenedConversation(openedConversationId = OPENED_ID)
                    mapWithOpenedConversation(openedConversationId = null)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun MockKMatcherScope.mapWithOpenedConversation(
        openedConversationId: ConversationId?,
    ): State {
        return uiStateMapper.map(
            snapshot = any(),
            selectedConversationIds = any(),
            openedConversationId = openedConversationId,
            isDebugEnabled = false,
        )
    }

    private fun createViewModel(): ArchivedConversationListViewModel {
        every { optimisticSnapshotDelegate.snapshot } returns snapshotFlow
        every { selectionDelegate.selectedIds } returns selectedIdsFlow
        every { uiStateMapper.map(any(), any(), any(), any()) } returns State()

        return ArchivedConversationListViewModel(
            actionsDelegate = mockk(relaxed = true),
            optimisticSnapshotDelegate = optimisticSnapshotDelegate,
            selectionDelegate = selectionDelegate,
            uiStateMapper = uiStateMapper,
            resolveContactAction = { _, _, _ -> ResolveContactActionResult.Unavailable },
            debugFeaturesProvider = { false },
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private companion object {
        private val OPENED_ID = ConversationId("opened")
    }
}
