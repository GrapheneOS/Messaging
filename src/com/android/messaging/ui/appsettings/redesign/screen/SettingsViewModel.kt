package com.android.messaging.ui.appsettings.redesign.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.appsettings.redesign.model.SettingsScreenEffect
import com.android.messaging.ui.appsettings.redesign.model.SettingsUiState
import com.android.messaging.ui.appsettings.redesign.subscription.delegate.SubscriptionSettingsDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal interface SettingsScreenModel {
    val effects: Flow<SettingsScreenEffect>
    val uiState: StateFlow<SettingsUiState>

    fun refreshState()
}

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val subscriptionSettingsDelegate: SubscriptionSettingsDelegate,
) : ViewModel(), SettingsScreenModel {

    private val _effects = MutableSharedFlow<SettingsScreenEffect>(extraBufferCapacity = 1)
    override val effects = _effects.asSharedFlow()

    override val uiState: StateFlow<SettingsUiState> = subscriptionSettingsDelegate.state
        .map { subscriptionState ->
            SettingsUiState(
                isMultiSim = subscriptionState.isMultiSim,
                subscriptionSettings = subscriptionState.subscriptions,
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
    }

    override fun refreshState() {
        subscriptionSettingsDelegate.refresh()
    }

    private companion object {
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
