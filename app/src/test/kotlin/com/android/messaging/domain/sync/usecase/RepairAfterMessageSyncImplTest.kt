package com.android.messaging.domain.sync.usecase

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

    private val participantDestinationNormalizer = mockk<ParticipantDestinationNormalizer>()

    private val repairAfterMessageSync = RepairAfterMessageSyncImpl(
        participantDestinationNormalizer = participantDestinationNormalizer,
    )

    @Before
    fun setUp() {
        mockkStatic(ParticipantRefresh::class)
        mockkStatic(BugleDatabaseOperations::class)
        mockkStatic(MessagingContentProvider::class)

        every { participantDestinationNormalizer.renormalizeIfPending() } returns false
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
    fun aFailedRenormalization_notifiesNoRepair() {
        every { participantDestinationNormalizer.renormalizeIfPending() } throws
            IllegalStateException("telephony gone")

        repairAfterMessageSync()

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

        verify(exactly = 0) { ParticipantRefresh.refreshParticipants(any()) }
        verify(exactly = 0) { MessagingContentProvider.notifyEverythingChanged() }
    }
}
