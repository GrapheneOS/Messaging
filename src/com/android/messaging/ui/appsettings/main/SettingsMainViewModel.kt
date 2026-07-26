package com.android.messaging.ui.appsettings.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.appsettings.subscription.delegate.SubscriptionSettingsDelegate
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
internal class SettingsMainViewModel @Inject constructor(
    private val subscriptionSettingsDelegate: SubscriptionSettingsDelegate,
) : ViewModel() {

    val uiState: StateFlow<SubscriptionSettingsUiState> = subscriptionSettingsDelegate.state

    init {
        subscriptionSettingsDelegate.bind(scope = viewModelScope)
    }

    fun refreshState() {
        subscriptionSettingsDelegate.refresh()
    }
}
