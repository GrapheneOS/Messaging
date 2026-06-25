package com.android.messaging.di.smsstoragelow

import com.android.messaging.domain.smsstoragelow.mapper.SmsStorageRetentionDurationMapper
import com.android.messaging.domain.smsstoragelow.mapper.SmsStorageRetentionDurationMapperImpl
import com.android.messaging.domain.smsstoragelow.usecase.CancelSmsStorageLowNotification
import com.android.messaging.domain.smsstoragelow.usecase.CancelSmsStorageLowNotificationImpl
import com.android.messaging.domain.smsstoragelow.usecase.GetSmsStorageLowWarningActions
import com.android.messaging.domain.smsstoragelow.usecase.GetSmsStorageLowWarningActionsImpl
import com.android.messaging.domain.smsstoragelow.usecase.ReleaseSmsStorage
import com.android.messaging.domain.smsstoragelow.usecase.ReleaseSmsStorageImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SmsStorageLowWarningBindsModule {

    @Binds
    @Reusable
    abstract fun bindSmsStorageRetentionDurationMapper(
        impl: SmsStorageRetentionDurationMapperImpl,
    ): SmsStorageRetentionDurationMapper

    @Binds
    @Reusable
    abstract fun bindGetSmsStorageLowWarningActions(
        impl: GetSmsStorageLowWarningActionsImpl,
    ): GetSmsStorageLowWarningActions

    @Binds
    @Reusable
    abstract fun bindReleaseSmsStorage(
        impl: ReleaseSmsStorageImpl,
    ): ReleaseSmsStorage

    @Binds
    @Reusable
    abstract fun bindCancelSmsStorageLowNotification(
        impl: CancelSmsStorageLowNotificationImpl,
    ): CancelSmsStorageLowNotification
}
