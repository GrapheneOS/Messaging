package com.android.messaging.ui.appsettings.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal interface SettingsScreenDelegate<T> {
    val state: StateFlow<T>

    fun bind(scope: CoroutineScope)
    fun refresh()
}
