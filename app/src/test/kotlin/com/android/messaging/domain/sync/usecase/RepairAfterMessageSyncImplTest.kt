package com.android.messaging.domain.sync.usecase

import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.ParticipantRefresh
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepairAfterMessageSyncImplTest {

    private val conversationStateMirror = mockk<ConversationStateMirror>(relaxed = true)
    private val participantDestinationNormalizer = mockk<ParticipantDestinationNormalizer>()

    private val repairAfterMessageSync = RepairAfterMessageSyncImpl(
        conversationStateMirror = conversationStateMirror,
        participantDestinationNormalizer = participantDestinationNormalizer,
    )

    @Before
    fun setUp() {
        mockkStatic(ParticipantRefresh::class)
        mockkStatic(BugleDatabaseOperations::class)
        mockkStatic(MessagingContentProvider::class)

        every { participantDestinationNormalizer.renormalizeIfPending() } returns false
        every { conversationStateMirror.restoreIfDue() } returns false
        every { conversationStateMirror.isRestorePending() } returns false
        every { ParticipantRefresh.refreshParticipants(any()) } just runs
        every { BugleDatabaseOperations.clearParticipantIdCache() } just runs
        every { MessagingContentProvider.notifyEverythingChanged() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun renormalizedParticipants_haveTheirContactsLookedUp() {
        every { participantDestinationNormalizer.renormalizeIfPending() } returns true

        repairAfterMessageSync()

        verifyOrder {
            BugleDatabaseOperations.clearParticipantIdCache()
            ParticipantRefresh.refreshParticipants(ParticipantRefresh.REFRESH_MODE_INCREMENTAL)
            MessagingContentProvider.notifyEverythingChanged()
        }
    }

    @Test
    fun restoredBlocks_haveTheirContactsLookedUp() {
        every { conversationStateMirror.restoreIfDue() } returns true

        repairAfterMessageSync()

        verify(exactly = 1) {
            ParticipantRefresh.refreshParticipants(ParticipantRefresh.REFRESH_MODE_INCREMENTAL)
        }
    }

    @Test
    fun theRestore_runsBeforeTheRenormalization() {
        repairAfterMessageSync()

        verifyOrder {
            conversationStateMirror.restoreIfDue()
            participantDestinationNormalizer.renormalizeIfPending()
            conversationStateMirror.update()
        }
    }

    @Test
    fun aRestoreStillPending_holdsBackTheRenormalization() {
        every { conversationStateMirror.isRestorePending() } returns true

        repairAfterMessageSync()

        verify(exactly = 0) { participantDestinationNormalizer.renormalizeIfPending() }
    }

    @Test
    fun aFailedRestore_stillRenormalizesAndMirrors() {
        every { conversationStateMirror.restoreIfDue() } throws
            IllegalStateException("database closed")

        repairAfterMessageSync()

        verify(exactly = 1) { participantDestinationNormalizer.renormalizeIfPending() }
        verify(exactly = 1) { conversationStateMirror.update() }
        verify(exactly = 0) { MessagingContentProvider.notifyEverythingChanged() }
    }

    @Test
    fun aFailedRenormalization_stillMirrors() {
        every { participantDestinationNormalizer.renormalizeIfPending() } throws
            IllegalStateException("telephony gone")

        repairAfterMessageSync()

        verify(exactly = 1) { conversationStateMirror.update() }
        verify(exactly = 0) { MessagingContentProvider.notifyEverythingChanged() }
    }

    @Test
    fun aFailedContactLookup_stillNotifiesTheRepair() {
        every { participantDestinationNormalizer.renormalizeIfPending() } returns true
        every { ParticipantRefresh.refreshParticipants(any()) } throws
            IllegalStateException("contacts provider gone")

        repairAfterMessageSync()

        verify(exactly = 1) { MessagingContentProvider.notifyEverythingChanged() }
    }

    @Test
    fun nothingRepaired_looksUpNoContacts() {
        repairAfterMessageSync()

        verify(exactly = 1) { conversationStateMirror.update() }
        verify(exactly = 0) { ParticipantRefresh.refreshParticipants(any()) }
        verify(exactly = 0) { MessagingContentProvider.notifyEverythingChanged() }
    }
}
