package com.android.messaging.ui.conversationlist.delegate

import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationlist.repository.ConversationListRepository
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.ui.conversationlist.conversationItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationListActionsDelegateImplTest {

    @Test
    fun setArchived_archivesEachDistinctNonBlankConversation() = runTest {
        val harness = createHarness()

        harness.delegate.setArchived(
            conversationIds = listOf("a", "", "a", "b"),
            isArchived = true,
        )

        coVerify(exactly = 1) { harness.conversationsRepository.archiveConversation("a") }
        coVerify(exactly = 1) { harness.conversationsRepository.archiveConversation("b") }
        coVerify(exactly = 0) { harness.conversationsRepository.unarchiveConversation(any()) }
    }

    @Test
    fun setArchived_unarchivesWhenNotArchived() = runTest {
        val harness = createHarness()

        harness.delegate.setArchived(
            conversationIds = listOf("a"),
            isArchived = false,
        )

        coVerify(exactly = 1) { harness.conversationsRepository.unarchiveConversation("a") }
        coVerify(exactly = 0) { harness.conversationsRepository.archiveConversation(any()) }
    }

    @Test
    fun setPinned_pinsEachDistinctNonBlankConversation() = runTest {
        val harness = createHarness()

        harness.delegate.setPinned(
            conversationIds = listOf("a", "", " ", "b", "a"),
            isPinned = true,
        )

        coVerify(exactly = 1) { harness.conversationsRepository.pinConversation("a") }
        coVerify(exactly = 1) { harness.conversationsRepository.pinConversation("b") }
        coVerify(exactly = 0) { harness.conversationsRepository.pinConversation("") }
        coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(" ") }
    }

    @Test
    fun setPinned_unpinsWhenNotPinned() = runTest {
        val harness = createHarness()

        harness.delegate.setPinned(
            conversationIds = listOf("a"),
            isPinned = false,
        )

        coVerify(exactly = 1) { harness.conversationsRepository.unpinConversation("a") }
        coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(any()) }
    }

    @Test
    fun setPinned_emptyIds_doesNothing() = runTest {
        val harness = createHarness()

        harness.delegate.setPinned(
            conversationIds = emptyList(),
            isPinned = true,
        )

        coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(any()) }
        coVerify(exactly = 0) { harness.conversationsRepository.unpinConversation(any()) }
    }

    @Test
    fun setPinned_blankOnlyIds_doesNothing() = runTest {
        val harness = createHarness()

        harness.delegate.setPinned(
            conversationIds = listOf("", "  "),
            isPinned = true,
        )

        coVerify(exactly = 0) { harness.conversationsRepository.pinConversation(any()) }
    }

    @Test
    fun setRead_marksEachDistinctNonBlankConversation() = runTest {
        val harness = createHarness()

        harness.delegate.setRead(
            conversationIds = listOf("a", "", " ", "b", "a"),
            isRead = true,
        )

        coVerify(exactly = 1) { harness.conversationsRepository.markConversationRead("a") }
        coVerify(exactly = 1) { harness.conversationsRepository.markConversationRead("b") }
        coVerify(exactly = 0) { harness.conversationsRepository.markConversationUnread(any()) }
    }

    @Test
    fun setRead_marksUnreadWhenNotRead() = runTest {
        val harness = createHarness()

        harness.delegate.setRead(
            conversationIds = listOf("a"),
            isRead = false,
        )

        coVerify(exactly = 1) { harness.conversationsRepository.markConversationUnread("a") }
        coVerify(exactly = 0) { harness.conversationsRepository.markConversationRead(any()) }
    }

    @Test
    fun snooze_snoozesEachDistinctNonBlankConversation() = runTest {
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

    @Test
    fun unsnooze_clearsEachDistinctNonBlankConversation() = runTest {
        val harness = createHarness()

        harness.delegate.unsnooze(listOf("a", "", " ", "b", "a"))

        verify(exactly = 1) { harness.conversationListRepository.clearSnooze("a") }
        verify(exactly = 1) { harness.conversationListRepository.clearSnooze("b") }
    }

    @Test
    fun block_returnsResultFromRepository() = runTest {
        val harness = createHarness()
        coEvery {
            harness.blockedParticipantsRepository.setDestinationBlocked(
                destination = "+15551234",
                conversationId = "conv",
                isBlocked = true,
            )
        } returns true

        val result = harness.delegate.block(
            conversationId = "conv",
            destination = "+15551234",
        )

        assertEquals(true, result)
        coVerify(exactly = 1) {
            harness.blockedParticipantsRepository.setDestinationBlocked(
                destination = "+15551234",
                conversationId = "conv",
                isBlocked = true,
            )
        }
    }

    @Test
    fun block_blankDestination_returnsFalseWithoutTouchingRepository() = runTest {
        val harness = createHarness()

        val result = harness.delegate.block(
            conversationId = "conv",
            destination = "  ",
        )

        assertEquals(false, result)
        coVerify(exactly = 0) {
            harness.blockedParticipantsRepository.setDestinationBlocked(any(), any(), any())
        }
    }

    @Test
    fun unblock_clearsBlockedState() = runTest {
        val harness = createHarness()
        coEvery {
            harness.blockedParticipantsRepository.setDestinationBlocked(
                destination = "+15551234",
                conversationId = "conv",
                isBlocked = false,
            )
        } returns true

        harness.delegate.unblock(
            conversationId = "conv",
            destination = "+15551234",
        )

        coVerify(exactly = 1) {
            harness.blockedParticipantsRepository.setDestinationBlocked(
                destination = "+15551234",
                conversationId = "conv",
                isBlocked = false,
            )
        }
    }

    @Test
    fun delete_routesEachItemWithItsLatestMessageTimestamp() = runTest {
        val harness = createHarness()

        harness.delegate.delete(
            listOf(
                conversationItem("a", timestamp = 5_000L),
                conversationItem("b", timestamp = 7_000L),
            ),
        )

        verify(exactly = 1) { harness.conversationsRepository.deleteConversation("a", 5_000L) }
        verify(exactly = 1) { harness.conversationsRepository.deleteConversation("b", 7_000L) }
    }

    private fun createHarness(): Harness {
        val conversationsRepository = mockk<ConversationsRepository>(relaxed = true)
        val conversationListRepository = mockk<ConversationListRepository>(relaxed = true)
        val blockedParticipantsRepository =
            mockk<BlockedParticipantsRepository>(relaxed = true)
        val delegate = ConversationListActionsDelegateImpl(
            conversationsRepository = conversationsRepository,
            conversationListRepository = conversationListRepository,
            blockedParticipantsRepository = blockedParticipantsRepository,
        )

        return Harness(
            conversationsRepository = conversationsRepository,
            conversationListRepository = conversationListRepository,
            blockedParticipantsRepository = blockedParticipantsRepository,
            delegate = delegate,
        )
    }

    private class Harness(
        val conversationsRepository: ConversationsRepository,
        val conversationListRepository: ConversationListRepository,
        val blockedParticipantsRepository: BlockedParticipantsRepository,
        val delegate: ConversationListActionsDelegateImpl,
    )
}
