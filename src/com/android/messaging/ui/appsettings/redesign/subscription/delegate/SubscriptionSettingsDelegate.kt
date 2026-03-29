package com.android.messaging.ui.appsettings.redesign.subscription.delegate

import android.content.Context
import com.android.messaging.R
import com.android.messaging.datamodel.ParticipantRefresh
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.appsettings.redesign.common.SettingsScreenDelegate
import com.android.messaging.ui.appsettings.redesign.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.appsettings.redesign.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SubscriptionSettingsState(
    val isMultiSim: Boolean = false,
    val subscriptions: List<SubscriptionSettingsUiState> = emptyList(),
)

internal interface SubscriptionSettingsDelegate :
    SettingsScreenDelegate<SubscriptionSettingsState> {
    fun onAutoRetrieveMmsChanged(subId: Int, enabled: Boolean)
    fun onAutoRetrieveMmsWhenRoamingChanged(subId: Int, enabled: Boolean)
    fun onDeliveryReportsChanged(subId: Int, enabled: Boolean)
    fun onGroupMmsChanged(subId: Int, enabled: Boolean)
    fun onPhoneNumberChanged(subId: Int, phoneNumber: String)
}

internal class SubscriptionSettingsDelegateImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapper: SubscriptionSettingsUiStateMapper,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : SubscriptionSettingsDelegate {

    private val _state = MutableStateFlow(SubscriptionSettingsState())
    override val state: StateFlow<SubscriptionSettingsState> = _state.asStateFlow()

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
            _state.value = SubscriptionSettingsState(
                isMultiSim = mapper.isMultiSim(),
                subscriptions = mapper.mapSubscriptions(),
            )
        }
    }

    override fun onAutoRetrieveMmsChanged(subId: Int, enabled: Boolean) {
        val key = context.getString(R.string.auto_retrieve_mms_pref_key)
        BuglePrefs.getSubscriptionPrefs(subId).putBoolean(key, enabled)
        refresh()
    }

    override fun onAutoRetrieveMmsWhenRoamingChanged(subId: Int, enabled: Boolean) {
        val key = context.getString(R.string.auto_retrieve_mms_when_roaming_pref_key)
        BuglePrefs.getSubscriptionPrefs(subId).putBoolean(key, enabled)
        refresh()
    }

    override fun onDeliveryReportsChanged(subId: Int, enabled: Boolean) {
        val key = context.getString(R.string.delivery_reports_pref_key)
        BuglePrefs.getSubscriptionPrefs(subId).putBoolean(key, enabled)
        refresh()
    }

    override fun onGroupMmsChanged(subId: Int, enabled: Boolean) {
        val key = context.getString(R.string.group_mms_pref_key)
        BuglePrefs.getSubscriptionPrefs(subId).putBoolean(key, enabled)
        refresh()
    }

    override fun onPhoneNumberChanged(subId: Int, phoneNumber: String) {
        val phoneUtils = PhoneUtils.get(subId)

        val canonical = phoneUtils.getCanonicalBySystemLocale(phoneNumber)
        val defaultCanonical = phoneUtils.getCanonicalBySystemLocale(
            phoneUtils.getCanonicalForSelf(false),
        )

        val key = context.getString(R.string.mms_phone_number_pref_key)
        val subPrefs = BuglePrefs.getSubscriptionPrefs(subId)
        if (canonical == defaultCanonical || phoneNumber.isEmpty()) {
            subPrefs.remove(key)
        } else {
            subPrefs.putString(key, phoneNumber)
        }

        ParticipantRefresh.refreshSelfParticipants()
        refresh()
    }
}
