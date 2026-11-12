package com.android.messaging.ui.conversationsettings.screen

import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.domain.conversation.usecase.participant.ResolveConversationId
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversationsettings.screen.delegate.ConversationSettingsDelegate
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavEvent as NavEvent
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val delegate = mockk<ConversationSettingsDelegate>(relaxed = true)

    @Test
    fun unarchiveClicked_unarchivesAndClosesAfterUnarchive() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.navigationEvents.test {
                viewModel.onAction(Action.UnarchiveClicked)
                advanceUntilIdle()

                assertEquals(NavEvent.CloseAfterUnarchive, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { delegate.setArchived(false) }
        }
    }

    @Test
    fun archiveClicked_archivesAndClosesAfterArchive() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.navigationEvents.test {
                viewModel.onAction(Action.ArchiveClicked)
                advanceUntilIdle()

                assertEquals(NavEvent.CloseAfterArchive, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { delegate.setArchived(true) }
        }
    }

    private fun createViewModel(): ConversationSettingsViewModel {
        every { delegate.state } returns MutableStateFlow(
            ConversationSettingsUiState(CONVERSATION_ID),
        )
        every { delegate.rootConversationId } returns CONVERSATION_ID

        return ConversationSettingsViewModel(
            delegate = delegate,
            resolveConversationId = mockk<ResolveConversationId>(relaxed = true),
            resolveContactAction = { _, _, _ -> error("Not expected") },
        )
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation-1")
    }
}
