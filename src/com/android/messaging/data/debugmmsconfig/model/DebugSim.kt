package com.android.messaging.data.debugmmsconfig.model

import com.android.messaging.data.subscription.model.SubId

internal data class DebugSim(
    val subId: SubId,
    val mcc: Int,
    val mnc: Int,
)
