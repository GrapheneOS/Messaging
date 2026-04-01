package com.android.messaging.ui.appsettings.redesign.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.appsettings.redesign.appsettings.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.redesign.subscription.delegate.SubscriptionSettingsDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.android.messaging.ui.appsettings.redesign.screen.model.SettingsScreenEffect as Effect
import com.android.messaging.ui.appsettings.redesign.screen.model.SettingsUiState as State

internal interface SettingsScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun refreshState()

    fun onAutoRetrieveMmsChanged(subId: Int, enabled: Boolean)
    fun onAutoRetrieveMmsWhenRoamingChanged(subId: Int, enabled: Boolean)
    fun onDeliveryReportsChanged(subId: Int, enabled: Boolean)
    fun onGroupMmsChanged(subId: Int, enabled: Boolean)
    fun onPhoneNumberChanged(subId: Int, phoneNumber: String)
    fun onWirelessAlertsClick(subId: Int)

    fun onDumpMmsChanged(enabled: Boolean)
    fun onDumpSmsChanged(enabled: Boolean)
    fun onSendSoundChanged(enabled: Boolean)
    fun onDefaultSmsAppClick(isCurrentlyDefault: Boolean)
    fun onNotificationsClick()

    fun onLicensesClick()
}

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val subscriptionSettingsDelegate: SubscriptionSettingsDelegate,
    private val appSettingsDelegate: AppSettingsDelegate,
) : ViewModel(), SettingsScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    override val uiState: StateFlow<State> = combine(
        subscriptionSettingsDelegate.state,
        appSettingsDelegate.state,
    ) { subscriptionState, appSettings ->
        State(
            isMultiSim = subscriptionState.isMultiSim,
            subscriptionSettings = subscriptionState.subscriptions,
            appSettings = appSettings,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATEFLOW_STOP_TIMEOUT_MILLIS),
        initialValue = State(),
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

    override fun onAutoRetrieveMmsChanged(subId: Int, enabled: Boolean) {
        subscriptionSettingsDelegate.onAutoRetrieveMmsChanged(subId, enabled)
    }

    override fun onAutoRetrieveMmsWhenRoamingChanged(subId: Int, enabled: Boolean) {
        subscriptionSettingsDelegate.onAutoRetrieveMmsWhenRoamingChanged(subId, enabled)
    }

    override fun onDeliveryReportsChanged(subId: Int, enabled: Boolean) {
        subscriptionSettingsDelegate.onDeliveryReportsChanged(subId, enabled)
    }

    override fun onGroupMmsChanged(subId: Int, enabled: Boolean) {
        subscriptionSettingsDelegate.onGroupMmsChanged(subId, enabled)
    }

    override fun onPhoneNumberChanged(subId: Int, phoneNumber: String) {
        subscriptionSettingsDelegate.onPhoneNumberChanged(subId, phoneNumber)
    }

    override fun onWirelessAlertsClick(subId: Int) {
        emitEffect(Effect.OpenWirelessAlerts(subId))
    }

    override fun onDumpMmsChanged(enabled: Boolean) {
        appSettingsDelegate.onDumpMmsChanged(enabled)
    }

    override fun onDumpSmsChanged(enabled: Boolean) {
        appSettingsDelegate.onDumpSmsChanged(enabled)
    }

    override fun onSendSoundChanged(enabled: Boolean) {
        appSettingsDelegate.onSendSoundChanged(enabled)
    }

    override fun onDefaultSmsAppClick(isCurrentlyDefault: Boolean) {
        val effect = if (isCurrentlyDefault) {
            Effect.OpenManageDefaultApps
        } else {
            Effect.RequestDefaultSmsApp
        }
        emitEffect(effect)
    }

    override fun onNotificationsClick() {
        emitEffect(Effect.OpenNotificationSettings)
    }

    override fun onLicensesClick() {
        emitEffect(Effect.OpenLicenses)
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
