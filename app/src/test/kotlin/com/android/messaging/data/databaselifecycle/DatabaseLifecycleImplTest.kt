package com.android.messaging.data.databaselifecycle

import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DatabaseLifecycleImplTest {

    private val participantDestinationNormalizer =
        mockk<ParticipantDestinationNormalizer>(relaxed = true)

    private val databaseLifecycle = DatabaseLifecycleImpl(
        participantDestinationNormalizer = participantDestinationNormalizer,
    )

    @Test
    fun aCreatedDatabase_reachesEveryListener() {
        databaseLifecycle.onDatabaseCreated()

        verify(exactly = 1) { participantDestinationNormalizer.onDatabaseCreated() }
    }

    @Test
    fun anUpgradedDatabase_reachesEveryListener() {
        databaseLifecycle.onDatabaseUpgraded(oldVersion = OLD_VERSION)

        verify(exactly = 1) {
            participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = OLD_VERSION)
        }
    }

    private companion object {
        const val OLD_VERSION = 3
    }
}
