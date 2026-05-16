package com.android.messaging.ui.appsettings.general.delegate

import com.android.messaging.data.appsettings.model.AppBooleanPref
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.ui.appsettings.common.SettingsScreenDelegate
import com.android.messaging.ui.appsettings.general.mapper.AppSettingsUiStateMapper
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface AppSettingsDelegate : SettingsScreenDelegate<AppSettingsUiState> {
    fun onSendSoundChanged(enabled: Boolean)
    fun onDumpSmsChanged(enabled: Boolean)
    fun onDumpMmsChanged(enabled: Boolean)
}

internal class AppSettingsDelegateImpl @Inject constructor(
    private val repository: AppSettingsRepository,
    private val mapper: AppSettingsUiStateMapper,
) : AppSettingsDelegate {

    private val _state = MutableStateFlow(AppSettingsUiState())
    override val state: StateFlow<AppSettingsUiState> = _state.asStateFlow()

    private val refreshTriggers: Channel<Unit> = Channel(Channel.CONFLATED)

    private var boundScope: CoroutineScope? = null

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch {
            refreshTriggers.receiveAsFlow()
                .onStart { emit(Unit) }
                .map {
                    val data = repository.getAppSettings()
                    mapper.map(data)
                }
                .collect { _state.value = it }
        }
    }

    override fun refresh() {
        refreshTriggers.trySend(Unit)
    }

    override fun onSendSoundChanged(enabled: Boolean) {
        setBooleanPref(
            pref = AppBooleanPref.SEND_SOUND,
            enabled = enabled,
        )
    }

    override fun onDumpSmsChanged(enabled: Boolean) {
        setBooleanPref(
            pref = AppBooleanPref.DUMP_SMS,
            enabled = enabled,
        )
    }

    override fun onDumpMmsChanged(enabled: Boolean) {
        setBooleanPref(
            pref = AppBooleanPref.DUMP_MMS,
            enabled = enabled,
        )
    }

    private fun setBooleanPref(
        pref: AppBooleanPref,
        enabled: Boolean,
    ) {
        boundScope?.launch {
            repository.setBooleanPref(
                pref = pref,
                enabled = enabled,
            )
            refresh()
        }
    }
}
