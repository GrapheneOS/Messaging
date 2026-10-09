package com.android.messaging.data.sms

import com.android.messaging.sms.mmsConfigLoadCount
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface MmsConfigLoadSource {
    fun observeLoads(): Flow<Unit>
}

internal class MmsConfigLoadSourceImpl @Inject constructor() : MmsConfigLoadSource {

    override fun observeLoads(): Flow<Unit> {
        return mmsConfigLoadCount.map {}
    }
}
