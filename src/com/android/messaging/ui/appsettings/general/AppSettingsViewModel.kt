package com.android.messaging.ui.appsettings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.general.model.AppSettingsAction as Action
import com.android.messaging.ui.appsettings.general.model.AppSettingsNavEvent as NavEvent
import com.android.messaging.ui.appsettings.general.model.AppSettingsScreenEffect as Effect
import com.android.messaging.ui.appsettings.general.model.AppSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
internal class AppSettingsViewModel @Inject constructor(
    private val appSettingsDelegate: AppSettingsDelegate,
) : ViewModel() {

    val uiState: StateFlow<AppSettingsUiState> = appSettingsDelegate.state

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val _navigationEvents = Channel<NavEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavEvent> = _navigationEvents.receiveAsFlow()

    init {
        appSettingsDelegate.bind(scope = viewModelScope)
    }

    fun refreshState() {
        appSettingsDelegate.refresh()
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.DumpMmsChanged -> appSettingsDelegate.onDumpMmsChanged(action.enabled)
            is Action.DumpSmsChanged -> appSettingsDelegate.onDumpSmsChanged(action.enabled)
            is Action.SendSoundChanged -> appSettingsDelegate.onSendSoundChanged(action.enabled)

            is Action.YouTubeLinkPreviewsChanged -> {
                appSettingsDelegate.onYouTubeLinkPreviewsChanged(action.enabled)
            }

            is Action.NotificationsClicked -> _effects.trySend(Effect.OpenNotificationSettings)

            is Action.DefaultSmsAppClicked -> {
                val effect = when {
                    action.isCurrentlyDefault -> Effect.OpenManageDefaultApps
                    else -> Effect.RequestDefaultSmsApp
                }
                _effects.trySend(effect)
            }

            is Action.LicensesClicked -> _navigationEvents.trySend(NavEvent.OpenLicenses)
        }
    }
}
