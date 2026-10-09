@file:JvmName("MmsConfigLoads")

package com.android.messaging.sms

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val completedLoadCount = MutableStateFlow(value = 0)

internal val mmsConfigLoadCount: StateFlow<Int> = completedLoadCount.asStateFlow()

internal fun onMmsConfigLoaded() {
    completedLoadCount.update { count -> count + 1 }
}
