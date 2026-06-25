@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.messaging.ui.smsstoragelow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.smsstoragelow.model.SmsStorageLowWarningAction
import com.android.messaging.domain.smsstoragelow.usecase.CancelSmsStorageLowNotification
import com.android.messaging.domain.smsstoragelow.usecase.GetSmsStorageLowWarningActions
import com.android.messaging.domain.smsstoragelow.usecase.ReleaseSmsStorage
import com.android.messaging.ui.smsstoragelow.model.SmsStorageLowWarningScreenEffect as Effect
import com.android.messaging.ui.smsstoragelow.model.SmsStorageLowWarningUiState as State
import com.android.messaging.util.LogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val SMS_STORAGE_LOW_WARNING_SELECTED_ACTION_STATE_KEY = "selected_action"

private const val TAG = LogUtil.BUGLE_TAG
private const val DELETE_MEDIA_MESSAGES_ACTION_STATE_VALUE = "delete_media_messages"
private const val DELETE_OLD_MESSAGES_ACTION_STATE_VALUE = "delete_old_messages"

internal interface SmsStorageLowWarningScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onActionClicked(action: SmsStorageLowWarningAction)
    fun onCleanupConfirmed()
    fun onConfirmationDismissed()
    fun onIgnoreClicked()
}

@HiltViewModel
internal class SmsStorageLowWarningViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getSmsStorageLowWarningActions: GetSmsStorageLowWarningActions,
    private val releaseSmsStorage: ReleaseSmsStorage,
    private val cancelSmsStorageLowNotification: CancelSmsStorageLowNotification,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ViewModel(),
    SmsStorageLowWarningScreenModel {

    private val effectsChannel = Channel<Effect>(capacity = Channel.BUFFERED)
    override val effects = effectsChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(State())
    override val uiState = _uiState.asStateFlow()

    init {
        loadActions()
    }

    private fun loadActions() {
        viewModelScope.launch(mainDispatcher) {
            getSmsStorageLowWarningActions()
                .catch { exception ->
                    LogUtil.e(TAG, "Failed to load SMS storage warning actions", exception)
                    emitEffect(effect = Effect.Finish)
                }
                .collect { actions ->
                    _uiState.update {
                        it.copy(
                            actions = actions,
                            selectedAction = restoreSelectedAction(
                                actions = actions,
                            ),
                            isLoading = false,
                        )
                    }
                }
        }
    }

    override fun onActionClicked(action: SmsStorageLowWarningAction) {
        if (uiState.value.isProcessing) {
            return
        }

        if (action !in uiState.value.actions) {
            return
        }

        savedStateHandle[SMS_STORAGE_LOW_WARNING_SELECTED_ACTION_STATE_KEY] = action
            .toSavedStateValue()

        _uiState.update { it.copy(selectedAction = action) }
    }

    override fun onCleanupConfirmed() {
        val selectedAction = uiState.value.selectedAction
        if (selectedAction == null || uiState.value.isProcessing) {
            return
        }

        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch(mainDispatcher) {
            releaseSmsStorage(selectedAction)
                .flatMapConcat {
                    cancelSmsStorageLowNotification()
                }
                .catch { exception ->
                    LogUtil.e(TAG, "Failed to release SMS storage", exception)
                }
                .onCompletion {
                    _uiState.update { it.copy(isProcessing = false) }
                    emitEffect(effect = Effect.Finish)
                }
                .collect {
                    clearSelectedAction()
                }
        }
    }

    override fun onConfirmationDismissed() {
        if (uiState.value.isProcessing) {
            return
        }

        clearSelectedAction()
    }

    override fun onIgnoreClicked() {
        if (uiState.value.isProcessing) {
            return
        }

        emitEffect(effect = Effect.Finish)
    }

    private fun restoreSelectedAction(
        actions: ImmutableList<SmsStorageLowWarningAction>,
    ): SmsStorageLowWarningAction? {
        return savedStateHandle
            .get<String>(SMS_STORAGE_LOW_WARNING_SELECTED_ACTION_STATE_KEY)
            ?.let { action -> actions.firstOrNull { it.toSavedStateValue() == action } }
    }

    private fun SmsStorageLowWarningAction.toSavedStateValue(): String {
        return when (this) {
            is SmsStorageLowWarningAction.DeleteMediaMessages -> {
                DELETE_MEDIA_MESSAGES_ACTION_STATE_VALUE
            }

            is SmsStorageLowWarningAction.DeleteOldMessages -> {
                DELETE_OLD_MESSAGES_ACTION_STATE_VALUE
            }
        }
    }

    private fun clearSelectedAction() {
        savedStateHandle.remove<String>(SMS_STORAGE_LOW_WARNING_SELECTED_ACTION_STATE_KEY)
        _uiState.update { it.copy(selectedAction = null) }
    }

    private fun emitEffect(effect: Effect) {
        viewModelScope.launch(mainDispatcher) {
            effectsChannel.send(element = effect)
        }
    }
}
