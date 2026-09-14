package com.android.messaging.data.debug

import com.android.messaging.util.DebugUtils
import javax.inject.Inject

internal fun interface DebugFeaturesProvider {
    fun isEnabled(): Boolean
}

internal class DebugFeaturesProviderImpl @Inject constructor() : DebugFeaturesProvider {

    override fun isEnabled(): Boolean {
        return DebugUtils.isDebugEnabled()
    }
}
