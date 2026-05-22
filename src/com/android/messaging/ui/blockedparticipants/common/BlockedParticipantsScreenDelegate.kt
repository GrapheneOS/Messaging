package com.android.messaging.ui.blockedparticipants.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal interface BlockedParticipantsScreenDelegate<T> {
    val state: StateFlow<T>

    fun bind(scope: CoroutineScope)
}
