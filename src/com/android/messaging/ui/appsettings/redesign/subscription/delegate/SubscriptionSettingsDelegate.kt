package com.android.messaging.ui.appsettings.redesign.subscription.delegate

import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.appsettings.redesign.common.SettingsScreenDelegate
import com.android.messaging.ui.appsettings.redesign.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.appsettings.redesign.subscription.model.SubscriptionSettingsUiState
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
    SettingsScreenDelegate<SubscriptionSettingsState>

internal class SubscriptionSettingsDelegateImpl @Inject constructor(
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
}
