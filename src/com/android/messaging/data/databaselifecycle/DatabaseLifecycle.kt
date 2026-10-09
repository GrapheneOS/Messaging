package com.android.messaging.data.databaselifecycle

import android.content.Context
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

internal interface DatabaseLifecycle {

    fun onDatabaseCreated()

    fun onDatabaseUpgraded(oldVersion: Int)

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
    private val participantDestinationNormalizer: ParticipantDestinationNormalizer,
) : DatabaseLifecycle {

    override fun onDatabaseCreated() {
        participantDestinationNormalizer.onDatabaseCreated()
    }

    override fun onDatabaseUpgraded(oldVersion: Int) {
        participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = oldVersion)
    }
}
