package com.android.messaging.ui.conversationlist.redesign.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListActionsDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun setPinned_pinsEachDistinctNonBlankConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.setPinned(
                conversationIds = listOf("a", "", " ", "b", "a"),
                isPinned = true,
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { harness.conversationsRepository.pinConversation("a") }
            coVerify(exactly = 1) { harness.conversationsRepository.pinConversation("b") }
            coVerify(exactly = 0) { harness.conversationsRepository.pinConversation("") }
            coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(" ") }
        }
    }

    @Test
    fun setPinned_unpinsWhenNotPinned() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.setPinned(
                conversationIds = listOf("a"),
                isPinned = false,
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { harness.conversationsRepository.unpinConversation("a") }
            coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(any()) }
        }
    }

    @Test
    fun setPinned_emptyIds_doesNothing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.setPinned(
                conversationIds = emptyList(),
                isPinned = true,
            )
            advanceUntilIdle()

            coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(any()) }
            coVerify(exactly = 0) { harness.conversationsRepository.unpinConversation(any()) }
        }
    }

    @Test
    fun setPinned_blankOnlyIds_doesNothing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.setPinned(
                conversationIds = listOf("", "  "),
                isPinned = true,
            )
            advanceUntilIdle()

            coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(any()) }
        }
    }

    @Test
    fun setRead_marksEachDistinctNonBlankConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.setRead(
                conversationIds = listOf("a", "", " ", "b", "a"),
                isRead = true,
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { harness.conversationsRepository.markConversationRead("a") }
            coVerify(exactly = 1) { harness.conversationsRepository.markConversationRead("b") }
            coVerify(exactly = 0) { harness.conversationsRepository.markConversationUnread(any()) }
        }
    }

    @Test
    fun setRead_marksUnreadWhenNotRead() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.setRead(
                conversationIds = listOf("a"),
                isRead = false,
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { harness.conversationsRepository.markConversationUnread("a") }
            coVerify(exactly = 0) { harness.conversationsRepository.markConversationRead(any()) }
        }
    }

    @Test
    fun snooze_snoozesEachDistinctNonBlankConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.snooze(
                conversationIds = listOf("a", "", " ", "b", "a"),
                option = SnoozeOption.OneHour,
            )

            verify(exactly = 1) {
                harness.conversationListRepository.snooze("a", SnoozeOption.OneHour)
            }
            verify(exactly = 1) {
                harness.conversationListRepository.snooze("b", SnoozeOption.OneHour)
            }
        }
    }

    @Test
    fun unsnooze_clearsEachDistinctNonBlankConversation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val harness = createHarness()

            harness.delegate.unsnooze(listOf("a", "", " ", "b", "a"))

            verify(exactly = 1) { harness.conversationListRepository.clearSnooze("a") }
            verify(exactly = 1) { harness.conversationListRepository.clearSnooze("b") }
        }
    }

    private fun createHarness(): Harness {
        val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)
        val conversationListRepository = mockk<ConversationListRepository>(relaxed = true)
        val delegate = ConversationListActionsDelegateImpl(
            conversationsRepository = conversationsRepository,
            conversationListRepository = conversationListRepository,
            blockedParticipantsRepository = mockk<BlockedParticipantsRepository>(relaxed = true),
        ).apply { bind(scope = TestScope(mainDispatcherRule.testDispatcher)) }

        return Harness(
            conversationsRepository = conversationsRepository,
            conversationListRepository = conversationListRepository,
            delegate = delegate,
        )
    }

    private class Harness(
        val conversationsRepository: ConversationsRepository,
        val conversationListRepository: ConversationListRepository,
        val delegate: ConversationListActionsDelegateImpl,
    )
}
