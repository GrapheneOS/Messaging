package com.android.messaging.ui.permissioncheck.screen

import androidx.lifecycle.ViewModel
import com.android.messaging.data.permissioncheck.RequiredPermissionsChecker
import com.android.messaging.domain.permissioncheck.model.PermissionRequest
import com.android.messaging.domain.permissioncheck.usecase.DeterminePermissionRequest
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckAction as Action
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckScreenEffect as Effect
import com.android.messaging.ui.permissioncheck.screen.model.PermissionCheckUiState as State
import com.android.messaging.util.core.ElapsedRealtimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface PermissionCheckScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
    fun onScreenResumed()
    fun onRequestResult()
}

@HiltViewModel
internal class PermissionCheckViewModel @Inject constructor(
    private val checker: RequiredPermissionsChecker,
    private val determinePermissionRequest: DeterminePermissionRequest,
    private val elapsedRealtimeProvider: ElapsedRealtimeProvider,
) : ViewModel(),
    PermissionCheckScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    private val _uiState = MutableStateFlow(State())
    override val uiState: StateFlow<State> = _uiState.asStateFlow()

    private var requestStartedAtMillis = 0L

    override fun onAction(action: Action) {
        when (action) {
            Action.NextClicked -> {
                handleNextClicked()
            }

            Action.SettingsClicked -> {
                emitEffect(Effect.OpenAppSettings)
            }
        }
    }

    override fun onScreenResumed() {
        if (checker.hasRequiredPermissions()) {
            emitEffect(Effect.Redirect)
        }
    }

    override fun onRequestResult() {
        if (checker.hasRequiredPermissions()) {
            emitEffect(Effect.Redirect)
            return
        }

        val elapsed = elapsedRealtimeProvider.elapsedRealtimeMillis() - requestStartedAtMillis
        if (elapsed < AUTOMATED_RESULT_THRESHOLD_MILLIS) {
            _uiState.update {
                it.copy(showSettingsGuidance = true)
            }
        }
    }

    private fun handleNextClicked() {
        when (val request = determinePermissionRequest()) {
            PermissionRequest.SmsRole -> {
                requestStartedAtMillis = elapsedRealtimeProvider.elapsedRealtimeMillis()
                emitEffect(Effect.RequestSmsRole)
            }

            is PermissionRequest.RuntimePermissions -> {
                requestStartedAtMillis = elapsedRealtimeProvider.elapsedRealtimeMillis()
                emitEffect(Effect.RequestRuntimePermissions(request.permissions))
            }

            PermissionRequest.AlreadyGranted -> {
                emitEffect(Effect.Redirect)
            }
        }
    }

    private fun emitEffect(effect: Effect) {
        _effects.tryEmit(effect)
    }

    private companion object {
        private const val AUTOMATED_RESULT_THRESHOLD_MILLIS = 250L
    }
}
