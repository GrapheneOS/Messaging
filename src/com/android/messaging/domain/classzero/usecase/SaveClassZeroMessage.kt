package com.android.messaging.domain.classzero.usecase

import android.content.ContentValues
import com.android.messaging.datamodel.action.ReceiveSmsMessageAction
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal interface SaveClassZeroMessage {
    operator fun invoke(messageValues: ContentValues): Flow<Unit>
}

internal class SaveClassZeroMessageImpl @Inject constructor(
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : SaveClassZeroMessage {

    override operator fun invoke(messageValues: ContentValues): Flow<Unit> {
        // Wrapped in a flow to simplify future ReceiveSmsMessageAction refactoring
        return unitFlow {
            ReceiveSmsMessageAction(
                ContentValues(messageValues),
            ).start()
        }.flowOn(ioDispatcher)
    }
}
