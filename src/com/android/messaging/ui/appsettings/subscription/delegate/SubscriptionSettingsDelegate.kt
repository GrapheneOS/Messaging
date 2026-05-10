package com.android.messaging.ui.appsettings.subscription.delegate

import android.content.Context
import android.telephony.SubscriptionManager
import com.android.messaging.R
import com.android.messaging.datamodel.ParticipantRefresh
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.appsettings.common.SettingsScreenDelegate
import com.android.messaging.ui.appsettings.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal data class SubscriptionSettingsState(
    val isMultiSim: Boolean? = null,
    val isLoaded: Boolean = false,
    val subscriptions: ImmutableList<SubscriptionSettingsUiState> = persistentListOf(),
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
    private val subscriptionManager: SubscriptionManager,
    private val mapper: SubscriptionSettingsUiStateMapper,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : SubscriptionSettingsDelegate {

    private val _state = MutableStateFlow(
        SubscriptionSettingsState(
            isMultiSim = mapper.isMultiSim(),
        ),
    )
    override val state: StateFlow<SubscriptionSettingsState> = _state.asStateFlow()

    private val refreshTriggers: Channel<Unit> = Channel(Channel.CONFLATED)

    private var isBound = false

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        scope.launch {
            merge(
                subscriptionsChangedFlow(),
                refreshTriggers.receiveAsFlow(),
            )
                .onStart { emit(Unit) }
                .conflate()
                .map { computeState() }
                .flowOn(defaultDispatcher)
                .collect { _state.value = it }
        }
    }

    override fun refresh() {
        refreshTriggers.trySend(Unit)
    }

    private fun subscriptionsChangedFlow(): Flow<Unit> {
        return callbackFlow {
            val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    trySend(Unit)
                }
            }
            subscriptionManager.addOnSubscriptionsChangedListener(context.mainExecutor, listener)
            awaitClose {
                subscriptionManager.removeOnSubscriptionsChangedListener(listener)
            }
        }
    }

    private fun computeState(): SubscriptionSettingsState {
        return SubscriptionSettingsState(
            isMultiSim = mapper.isMultiSim(),
            isLoaded = true,
            subscriptions = mapper.mapSubscriptions(),
        )
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
