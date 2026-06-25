package com.android.messaging.domain.smsstoragelow.usecase

import com.android.messaging.datamodel.action.HandleLowStorageAction
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface ReleaseSmsStorage {
    operator fun invoke(action: SmsStorageLowWarningAction): Flow<Unit>
}

internal class ReleaseSmsStorageImpl @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ReleaseSmsStorage {

    override operator fun invoke(action: SmsStorageLowWarningAction): Flow<Unit> {
        return unitFlow {
            when (action) {
                is SmsStorageLowWarningAction.DeleteMediaMessages -> {
                    HandleLowStorageAction.handleDeleteMediaMessages(
                        action.retentionDuration.millis,
                    )
                }

                is SmsStorageLowWarningAction.DeleteOldMessages -> {
                    HandleLowStorageAction.handleDeleteOldMessages(
                        action.retentionDuration.millis,
                    )
                }
            }
        }.flowOn(ioDispatcher)
    }
}
