package com.android.messaging.data.databaselifecycle

import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test

class DatabaseLifecycleImplTest {

    private val conversationStateMirror = mockk<ConversationStateMirror>(relaxed = true)
    private val participantDestinationNormalizer =
        mockk<ParticipantDestinationNormalizer>(relaxed = true)

    private val databaseLifecycle = DatabaseLifecycleImpl(
        conversationStateMirror = conversationStateMirror,
        participantDestinationNormalizer = participantDestinationNormalizer,
    )

    @Test
    fun aCreatedDatabase_reachesEveryListener() {
        databaseLifecycle.onDatabaseCreated()

        verifyOrder {
            conversationStateMirror.onDatabaseCreated()
            participantDestinationNormalizer.onDatabaseCreated()
        }
    }

    @Test
    fun anUpgradedDatabase_reachesEveryListener() {
        databaseLifecycle.onDatabaseUpgraded(oldVersion = OLD_VERSION)

        verifyOrder {
            conversationStateMirror.onDatabaseUpgraded(oldVersion = OLD_VERSION)
            participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = OLD_VERSION)
        }
    }

    private companion object {
        const val OLD_VERSION = 3
    }
}
