package com.android.messaging.domain.smsstoragelow.usecase

import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.smsstoragelow.mapper.SmsStorageRetentionDurationMapper
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.sms.SmsReleaseStorage
import com.android.messaging.util.core.extension.typedFlow
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface GetSmsStorageLowWarningActions {
    operator fun invoke(): Flow<ImmutableList<SmsStorageLowWarningAction>>
}

internal class GetSmsStorageLowWarningActionsImpl @Inject constructor(
    private val smsStorageRetentionDurationMapper: SmsStorageRetentionDurationMapper,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetSmsStorageLowWarningActions {

    override operator fun invoke(): Flow<ImmutableList<SmsStorageLowWarningAction>> {
        return typedFlow {
            val retentionDuration = SmsReleaseStorage
                .parseMessageRetainingDuration()
                .let(smsStorageRetentionDurationMapper::map)

            persistentListOf(
                SmsStorageLowWarningAction.DeleteMediaMessages(
                    retentionDuration = retentionDuration,
                ),
                SmsStorageLowWarningAction.DeleteOldMessages(
                    retentionDuration = retentionDuration,
                ),
            )
        }.flowOn(ioDispatcher)
    }
}
