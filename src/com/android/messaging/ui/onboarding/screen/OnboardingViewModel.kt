package com.android.messaging.ui.onboarding.screen

import androidx.lifecycle.ViewModel
import com.android.messaging.data.onboarding.GetMissingPermissionLabels
import com.android.messaging.data.onboarding.RequiredPermissionsChecker
import com.android.messaging.data.onboarding.store.SmsWarningStore
import com.android.messaging.domain.onboarding.model.PermissionRequest
import com.android.messaging.domain.onboarding.usecase.DeterminePermissionRequest
import com.android.messaging.ui.onboarding.screen.model.OnboardingAction as Action
import com.android.messaging.ui.onboarding.screen.model.OnboardingScreenEffect as Effect
import com.android.messaging.ui.onboarding.screen.model.OnboardingUiState as State
import com.android.messaging.ui.onboarding.screen.model.SettingsGuidance
import com.android.messaging.util.core.ElapsedRealtimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface OnboardingScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
    fun onScreenResumed()
    fun onRequestResult()
}

@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val checker: RequiredPermissionsChecker,
    private val determinePermissionRequest: DeterminePermissionRequest,
    private val getMissingPermissionLabels: GetMissingPermissionLabels,
    private val elapsedRealtimeProvider: ElapsedRealtimeProvider,
    private val smsWarningStore: SmsWarningStore,
) : ViewModel(),
    OnboardingScreenModel {

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
    override val effects: Flow<Effect> = _effects.asSharedFlow()

    private val _uiState = MutableStateFlow(
        when {
            smsWarningStore.isAcknowledged() -> State.Permissions()
            else -> State.SmsWarning
        },
    )
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
        if (_uiState.value !is State.Permissions) {
            return
        }

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
            val guidance = when {
                !checker.isSmsRoleHeld() -> SettingsGuidance.DefaultSmsApp
                else -> SettingsGuidance.Permissions
            }
            val missingPermissions = when (guidance) {
                SettingsGuidance.DefaultSmsApp -> persistentListOf()
                SettingsGuidance.Permissions -> getMissingPermissionLabels()
            }

            _uiState.value = State.Permissions(
                settingsGuidance = guidance,
                missingPermissions = missingPermissions,
            )
        }
    }

    private fun handleNextClicked() {
        when (_uiState.value) {
            is State.SmsWarning -> handleSmsWarningAcknowledged()
            is State.Permissions -> handlePermissionRequest()
        }
    }

    private fun handleSmsWarningAcknowledged() {
        smsWarningStore.acknowledge()

        if (checker.hasRequiredPermissions()) {
            emitEffect(Effect.Redirect)
            return
        }

        _uiState.value = State.Permissions()
    }

    private fun handlePermissionRequest() {
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
