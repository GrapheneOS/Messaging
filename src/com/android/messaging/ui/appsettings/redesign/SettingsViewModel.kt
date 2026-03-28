package com.android.messaging.ui.appsettings.redesign

import androidx.lifecycle.ViewModel
import com.android.messaging.ui.appsettings.redesign.model.SettingsScreenEffect
import com.android.messaging.ui.appsettings.redesign.model.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal interface SettingsScreenModel {
    val effects: Flow<SettingsScreenEffect>
    val uiState: StateFlow<SettingsUiState>
}

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
): ViewModel(), SettingsScreenModel  {

    private val _effects = MutableSharedFlow<SettingsScreenEffect>(extraBufferCapacity = 1)
    override val effects = _effects.asSharedFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    override val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

}
