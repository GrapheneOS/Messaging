package com.android.messaging.ui.appsettings.general.delegate

import android.content.Context
import com.android.messaging.R
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.appsettings.common.SettingsScreenDelegate
import com.android.messaging.ui.appsettings.general.mapper.AppSettingsUiStateMapper
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import com.android.messaging.util.BuglePrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface AppSettingsDelegate : SettingsScreenDelegate<AppSettingsUiState> {
    fun onSendSoundChanged(enabled: Boolean)
    fun onDumpSmsChanged(enabled: Boolean)
    fun onDumpMmsChanged(enabled: Boolean)
}

internal class AppSettingsDelegateImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapper: AppSettingsUiStateMapper,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : AppSettingsDelegate {

    private val _state = MutableStateFlow(AppSettingsUiState())
    override val state: StateFlow<AppSettingsUiState> = _state.asStateFlow()

    private var boundScope: CoroutineScope? = null
    private var isBound = false

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true
        boundScope = scope
        refresh()
    }

    override fun refresh() {
        val scope = boundScope ?: return
        scope.launch(defaultDispatcher) {
            _state.value = mapper.map()
        }
    }

    override fun onSendSoundChanged(enabled: Boolean) {
        val key = context.getString(R.string.send_sound_pref_key)
        BuglePrefs.getApplicationPrefs().putBoolean(key, enabled)
        refresh()
    }

    override fun onDumpSmsChanged(enabled: Boolean) {
        val key = context.getString(R.string.dump_sms_pref_key)
        BuglePrefs.getApplicationPrefs().putBoolean(key, enabled)
        refresh()
    }

    override fun onDumpMmsChanged(enabled: Boolean) {
        val key = context.getString(R.string.dump_mms_pref_key)
        BuglePrefs.getApplicationPrefs().putBoolean(key, enabled)
        refresh()
    }
}
