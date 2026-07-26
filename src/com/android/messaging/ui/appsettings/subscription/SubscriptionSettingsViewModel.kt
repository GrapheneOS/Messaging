package com.android.messaging.ui.appsettings.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsAction as Action
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsNavEvent as NavEvent
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsScreenEffect as Effect
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal const val SUBSCRIPTION_SETTINGS_SUB_ID_ARG = "subId"

@HiltViewModel
internal class SubscriptionSettingsViewModel @Inject constructor(
    private val subscriptionSettingsDelegate: SubscriptionSettingsDelegate,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val subId = SubId(
        requireNotNull(
            savedStateHandle[SUBSCRIPTION_SETTINGS_SUB_ID_ARG],
        ) { "subId is required" },
    )

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val _navigationEvents = Channel<NavEvent>(Channel.BUFFERED)
    val navigationEvents: Flow<NavEvent> = _navigationEvents.receiveAsFlow()

    val uiState: StateFlow<SubscriptionUiState?> = subscriptionSettingsDelegate.state
        .map(::subscriptionOrNull)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATEFLOW_STOP_TIMEOUT_MILLIS),
            initialValue = subscriptionOrNull(subscriptionSettingsDelegate.state.value),
        )

    init {
        subscriptionSettingsDelegate.bind(scope = viewModelScope)
        closeWhenSubscriptionRemoved()
    }

    fun refreshState() {
        subscriptionSettingsDelegate.refresh()
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.AutoRetrieveMmsChanged -> {
                subscriptionSettingsDelegate.onAutoRetrieveMmsChanged(subId, action.enabled)
            }

            is Action.AutoRetrieveMmsWhenRoamingChanged -> {
                subscriptionSettingsDelegate.onAutoRetrieveMmsWhenRoamingChanged(
                    subId = subId,
                    enabled = action.enabled,
                )
            }

            is Action.DeliveryReportsChanged -> {
                subscriptionSettingsDelegate.onDeliveryReportsChanged(subId, action.enabled)
            }

            is Action.GroupMmsChanged -> {
                subscriptionSettingsDelegate.onGroupMmsChanged(subId, action.enabled)
            }

            is Action.PhoneNumberChanged -> {
                subscriptionSettingsDelegate.onPhoneNumberChanged(subId, action.phoneNumber)
            }

            Action.WirelessAlertsClicked -> {
                _effects.trySend(Effect.OpenWirelessAlerts)
            }
        }
    }

    private fun subscriptionOrNull(state: SubscriptionSettingsUiState): SubscriptionUiState? {
        return state.subscriptions.find { it.subId == subId }
    }

    private fun closeWhenSubscriptionRemoved() {
        viewModelScope.launch {
            subscriptionSettingsDelegate.state.first { state ->
                state.isLoaded && subscriptionOrNull(state) == null
            }
            _navigationEvents.trySend(NavEvent.Close)
        }
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
