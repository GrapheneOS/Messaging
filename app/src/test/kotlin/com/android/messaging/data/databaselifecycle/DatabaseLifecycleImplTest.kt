package com.android.messaging.data.databaselifecycle

import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.databasecompatibility.DatabaseCompatibility
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseLifecycleImplTest {

    private val conversationStateMirror = mockk<ConversationStateMirror>(relaxed = true)
    private val participantDestinationNormalizer =
        mockk<ParticipantDestinationNormalizer>(relaxed = true)
    private val databaseCompatibility = mockk<DatabaseCompatibility>(relaxed = true)

    private val databaseLifecycle = DatabaseLifecycleImpl(
        conversationStateMirror = conversationStateMirror,
        participantDestinationNormalizer = participantDestinationNormalizer,
        databaseCompatibility = databaseCompatibility,
    )

    @Test
    fun aCreatedDatabase_reachesEveryListenerAndRecordsTheVersionLast() {
        databaseLifecycle.onDatabaseCreated()

        verifyOrder {
            conversationStateMirror.onDatabaseCreated()
            participantDestinationNormalizer.onDatabaseCreated()
            databaseCompatibility.recordDatabaseVersion()
        }
    }

    @Test
    fun anUpgradedDatabase_reachesEveryListenerAndRecordsTheVersionLast() {
        databaseLifecycle.onDatabaseUpgraded(oldVersion = OLD_VERSION)

        verifyOrder {
            conversationStateMirror.onDatabaseUpgraded(oldVersion = OLD_VERSION)
            participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = OLD_VERSION)
            databaseCompatibility.recordDatabaseVersion()
        }
    }

    @Test
    fun aDowngrade_keepsTheRowsWhenTheCompatibilityRecordAllowsIt() {
        every {
            databaseCompatibility.canKeepRowsOnDowngradeFrom(newerVersion = NEWER_VERSION)
        } returns true

        assertTrue(databaseLifecycle.canKeepRowsOnDowngradeFrom(newerVersion = NEWER_VERSION))
    }

    private companion object {
        const val OLD_VERSION = 3
        const val NEWER_VERSION = 9
    }
}
