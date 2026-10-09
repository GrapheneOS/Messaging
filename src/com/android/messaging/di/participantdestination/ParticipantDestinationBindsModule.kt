package com.android.messaging.di.participantdestination

import com.android.messaging.data.participantdestination.BareNumberReader
import com.android.messaging.data.participantdestination.BareNumberReaderImpl
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizerImpl
import com.android.messaging.data.participantdestination.store.ParticipantDestinationDatabaseStore
import com.android.messaging.data.participantdestination.store.ParticipantDestinationDatabaseStoreImpl
import com.android.messaging.data.participantdestination.store.ParticipantDestinationPreferencesStore
import com.android.messaging.data.participantdestination.store.ParticipantDestinationPreferencesStoreImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ParticipantDestinationBindsModule {

    @Binds
    @Reusable
    abstract fun bindParticipantDestinationNormalizer(
        impl: ParticipantDestinationNormalizerImpl,
    ): ParticipantDestinationNormalizer

    @Binds
    @Reusable
    abstract fun bindBareNumberReader(
        impl: BareNumberReaderImpl,
    ): BareNumberReader

    @Binds
    @Reusable
    abstract fun bindParticipantDestinationDatabaseStore(
        impl: ParticipantDestinationDatabaseStoreImpl,
    ): ParticipantDestinationDatabaseStore

    // Singleton: it owns the lock the normalizer and the recorder share
    @Binds
    @Singleton
    abstract fun bindParticipantDestinationPreferencesStore(
        impl: ParticipantDestinationPreferencesStoreImpl,
    ): ParticipantDestinationPreferencesStore
}
