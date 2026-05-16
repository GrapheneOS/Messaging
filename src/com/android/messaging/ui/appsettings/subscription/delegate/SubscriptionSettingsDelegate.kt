package com.android.messaging.ui.appsettings.subscription.delegate

import com.android.messaging.data.subscriptionsettings.model.SubscriptionBooleanPref
import com.android.messaging.data.subscriptionsettings.repository.SubscriptionSettingsRepository
import com.android.messaging.domain.subscriptionsettings.usecase.SetSubscriptionPhoneNumber
import com.android.messaging.ui.appsettings.common.SettingsScreenDelegate
import com.android.messaging.ui.appsettings.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface SubscriptionSettingsDelegate :
    SettingsScreenDelegate<SubscriptionSettingsUiState> {
    fun onAutoRetrieveMmsChanged(subId: Int, enabled: Boolean)
    fun onAutoRetrieveMmsWhenRoamingChanged(subId: Int, enabled: Boolean)
    fun onDeliveryReportsChanged(subId: Int, enabled: Boolean)
    fun onGroupMmsChanged(subId: Int, enabled: Boolean)
    fun onPhoneNumberChanged(subId: Int, phoneNumber: String)
}

internal class SubscriptionSettingsDelegateImpl @Inject constructor(
    private val repository: SubscriptionSettingsRepository,
    private val setSubscriptionPhoneNumber: SetSubscriptionPhoneNumber,
    private val mapper: SubscriptionSettingsUiStateMapper,
) : SubscriptionSettingsDelegate {

    private val _state = MutableStateFlow(
        SubscriptionSettingsUiState(
            isMultiSim = repository.isMultiSim(),
        ),
    )
    override val state: StateFlow<SubscriptionSettingsUiState> = _state.asStateFlow()

    private val refreshTriggers: Channel<Unit> = Channel(Channel.CONFLATED)

    private var boundScope: CoroutineScope? = null

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch {
            merge(
                repository.observeSubscriptionsChanged(),
                refreshTriggers.receiveAsFlow(),
            )
                .onStart { emit(Unit) }
                .conflate()
                .map {
                    val data = repository.getSubscriptionSettings()
                    mapper.map(data)
                }
                .collect { _state.value = it }
        }
    }

    override fun refresh() {
        refreshTriggers.trySend(Unit)
    }

    override fun onAutoRetrieveMmsChanged(
        subId: Int,
        enabled: Boolean,
    ) {
        setBooleanPref(
            subId = subId,
            pref = SubscriptionBooleanPref.AUTO_RETRIEVE_MMS,
            enabled = enabled,
        )
    }

    override fun onAutoRetrieveMmsWhenRoamingChanged(
        subId: Int,
        enabled: Boolean,
    ) {
        setBooleanPref(
            subId = subId,
            pref = SubscriptionBooleanPref.AUTO_RETRIEVE_MMS_WHEN_ROAMING,
            enabled = enabled,
        )
    }

    override fun onDeliveryReportsChanged(
        subId: Int,
        enabled: Boolean,
    ) {
        setBooleanPref(
            subId = subId,
            pref = SubscriptionBooleanPref.DELIVERY_REPORTS,
            enabled = enabled,
        )
    }

    override fun onGroupMmsChanged(
        subId: Int,
        enabled: Boolean,
    ) {
        setBooleanPref(
            subId = subId,
            pref = SubscriptionBooleanPref.GROUP_MMS,
            enabled = enabled,
        )
    }

    override fun onPhoneNumberChanged(
        subId: Int,
        phoneNumber: String,
    ) {
        boundScope?.launch {
            setSubscriptionPhoneNumber(
                subId = subId,
                phoneNumber = phoneNumber,
            )
            refresh()
        }
    }

    private fun setBooleanPref(
        subId: Int,
        pref: SubscriptionBooleanPref,
        enabled: Boolean,
    ) {
        boundScope?.launch {
            repository.setSubscriptionBooleanPref(
                subId = subId,
                pref = pref,
                enabled = enabled,
            )
            refresh()
        }
    }
}
