package com.android.messaging.ui.appsettings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.appsettings.general.delegate.AppSettingsDelegate
import com.android.messaging.ui.appsettings.screen.model.SettingsScreenEffect
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
    val effects: Flow<SettingsScreenEffect>
    val uiState: StateFlow<SettingsUiState>

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
) : ViewModel(),
    SettingsScreenModel {

    private val _effects = MutableSharedFlow<SettingsScreenEffect>(extraBufferCapacity = 1)
    override val effects: Flow<SettingsScreenEffect> = _effects.asSharedFlow()

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
        emitEffect(SettingsScreenEffect.OpenWirelessAlerts(subId))
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
            SettingsScreenEffect.OpenManageDefaultApps
        } else {
            SettingsScreenEffect.RequestDefaultSmsApp
        }
        emitEffect(effect)
    }

    override fun onNotificationsClick() {
        emitEffect(SettingsScreenEffect.OpenNotificationSettings)
    }

    override fun onLicensesClick() {
        emitEffect(SettingsScreenEffect.OpenLicenses)
    }

    private fun emitEffect(effect: SettingsScreenEffect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
