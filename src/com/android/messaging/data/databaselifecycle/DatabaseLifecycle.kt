package com.android.messaging.data.databaselifecycle

import android.content.Context
import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.databasecompatibility.DatabaseCompatibility
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

internal interface DatabaseLifecycle {

    fun onDatabaseCreated()

    fun onDatabaseUpgraded(oldVersion: Int)

    fun canKeepRowsOnDowngradeFrom(newerVersion: Int): Boolean

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun databaseLifecycle(): DatabaseLifecycle
    }

    companion object {

        @JvmStatic
        fun get(context: Context): DatabaseLifecycle {
            return EntryPointAccessors
                .fromApplication(context, Provider::class.java)
                .databaseLifecycle()
        }
    }
}

internal class DatabaseLifecycleImpl @Inject constructor(
    private val conversationStateMirror: ConversationStateMirror,
    private val participantDestinationNormalizer: ParticipantDestinationNormalizer,
    private val databaseCompatibility: DatabaseCompatibility,
) : DatabaseLifecycle {

    override fun onDatabaseCreated() {
        conversationStateMirror.onDatabaseCreated()
        participantDestinationNormalizer.onDatabaseCreated()
        databaseCompatibility.recordDatabaseVersion()
    }

    override fun onDatabaseUpgraded(oldVersion: Int) {
        conversationStateMirror.onDatabaseUpgraded(oldVersion = oldVersion)
        participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = oldVersion)
        databaseCompatibility.recordDatabaseVersion()
    }

    override fun canKeepRowsOnDowngradeFrom(newerVersion: Int): Boolean {
        return databaseCompatibility.canKeepRowsOnDowngradeFrom(newerVersion = newerVersion)
    }
}
