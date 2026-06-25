package com.android.messaging.domain.smsstoragelow.usecase

import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.sms.SmsStorageStatusManager
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface CancelSmsStorageLowNotification {
    operator fun invoke(): Flow<Unit>
}

internal class CancelSmsStorageLowNotificationImpl @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CancelSmsStorageLowNotification {

    override operator fun invoke(): Flow<Unit> {
        return unitFlow {
            SmsStorageStatusManager.cancelStorageLowNotification()
        }.flowOn(ioDispatcher)
    }
}
