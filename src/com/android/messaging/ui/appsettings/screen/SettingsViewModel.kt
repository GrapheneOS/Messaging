package com.android.messaging.ui.appsettings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.screen.model.SettingsAction as Action
import com.android.messaging.ui.appsettings.screen.model.SettingsScreenEffect as Effect
import com.android.messaging.ui.appsettings.screen.model.SettingsUiState
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface SettingsScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<SettingsUiState>

    fun refreshState()
    fun onAction(action: Action)
}

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val subscriptionSettingsDelegate: SubscriptionSettingsDelegate,
    private val appSettingsDelegate: AppSettingsDelegate,
) : ViewModel(),
    SettingsScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<SettingsUiState> = combine(
        subscriptionSettingsDelegate.state,
        appSettingsDelegate.state,
    ) { subscriptionState, appSettings ->
        SettingsUiState(
            isMultiSim = subscriptionState.isMultiSim,
            subscriptionSettings = subscriptionState.subscriptions,
            appSettings = appSettings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATEFLOW_STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    init {
        initializeDelegates()
    }

    private fun initializeDelegates() {
        subscriptionSettingsDelegate.bind(scope = viewModelScope)
        appSettingsDelegate.bind(scope = viewModelScope)
    }

    override fun refreshState() {
        subscriptionSettingsDelegate.refresh()
        appSettingsDelegate.refresh()
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.AutoRetrieveMmsChanged -> {
                subscriptionSettingsDelegate.onAutoRetrieveMmsChanged(
                    subId = action.subId,
                    enabled = action.enabled,
                )
            }

            is Action.AutoRetrieveMmsWhenRoamingChanged -> {
                subscriptionSettingsDelegate.onAutoRetrieveMmsWhenRoamingChanged(
                    subId = action.subId,
                    enabled = action.enabled,
                )
            }

            is Action.DeliveryReportsChanged -> {
                subscriptionSettingsDelegate.onDeliveryReportsChanged(
                    subId = action.subId,
                    enabled = action.enabled,
                )
            }

            is Action.GroupMmsChanged -> {
                subscriptionSettingsDelegate.onGroupMmsChanged(
                    subId = action.subId,
                    enabled = action.enabled,
                )
            }

            is Action.PhoneNumberChanged -> {
                subscriptionSettingsDelegate.onPhoneNumberChanged(
                    subId = action.subId,
                    phoneNumber = action.phoneNumber,
                )
            }

            is Action.WirelessAlertsClicked -> {
                emitEffect(Effect.OpenWirelessAlerts(action.subId))
            }

            is Action.DumpMmsChanged -> {
                appSettingsDelegate.onDumpMmsChanged(action.enabled)
            }

            is Action.DumpSmsChanged -> {
                appSettingsDelegate.onDumpSmsChanged(action.enabled)
            }

            is Action.SendSoundChanged -> {
                appSettingsDelegate.onSendSoundChanged(action.enabled)
            }

            is Action.DefaultSmsAppClicked -> {
                val effect = if (action.isCurrentlyDefault) {
                    Effect.OpenManageDefaultApps
                } else {
                    Effect.RequestDefaultSmsApp
                }
                emitEffect(effect)
            }

            is Action.NotificationsClicked -> {
                emitEffect(Effect.OpenNotificationSettings)
            }

            is Action.LicensesClicked -> {
                emitEffect(Effect.OpenLicenses)
            }
        }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
