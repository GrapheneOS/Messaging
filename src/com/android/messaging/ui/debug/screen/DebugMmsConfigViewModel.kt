package com.android.messaging.ui.debug.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.debugmmsconfig.model.DebugSim
import com.android.messaging.data.debugmmsconfig.model.MmsConfigKeyType
import com.android.messaging.data.debugmmsconfig.repository.MmsConfigRepository
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.ui.debug.screen.mapper.DebugMmsConfigUiStateMapper
import com.android.messaging.ui.debug.screen.model.DebugMmsConfigAction as Action
import com.android.messaging.ui.debug.screen.model.DebugMmsConfigUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface DebugMmsConfigScreenModel {
    val uiState: StateFlow<State>

    fun onAction(action: Action)
}

@HiltViewModel
internal class DebugMmsConfigViewModel @Inject constructor(
    private val repository: MmsConfigRepository,
    private val mapper: DebugMmsConfigUiStateMapper,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(),
    DebugMmsConfigScreenModel {

    private val selectedSubId = savedStateHandle.getStateFlow(KEY_SELECTED_SUB_ID, UNSET_SUB_ID)

    private val refreshTriggers: Channel<Unit> = Channel(Channel.CONFLATED)

    private val activeSims: Flow<ImmutableList<DebugSim>> = flow {
        emit(repository.getActiveSims())
    }

    override val uiState: StateFlow<State> = combine(
        activeSims,
        selectedSubId,
        refreshTriggers.receiveAsFlow().onStart { emit(Unit) },
    ) { sims, selected, _ ->
        buildState(
            sims = sims,
            subId = resolveSubId(
                sims = sims,
                selected = selected,
            ),
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
            ),
            initialValue = State(),
        )

    override fun onAction(action: Action) {
        when (action) {
            is Action.SimSelected -> {
                savedStateHandle[KEY_SELECTED_SUB_ID] = action.subId.value
            }

            is Action.EntryToggled -> {
                updateEntry(
                    key = action.key,
                    keyType = MmsConfigKeyType.BOOL,
                    value = action.checked.toString(),
                )
            }

            is Action.EntryValueSubmitted -> {
                updateEntry(
                    key = action.key,
                    keyType = editableKeyType(isNumeric = action.isNumeric),
                    value = action.value,
                )
            }
        }
    }

    private suspend fun buildState(
        sims: ImmutableList<DebugSim>,
        subId: SubId?,
    ): State {
        val simItems = mapper.toSims(sims)

        if (subId == null) {
            return State(
                sims = simItems,
                isLoading = false,
            )
        }

        val entries = repository.getEntries(subId)
        val items = mapper.toItems(entries)

        return State(
            items = items,
            sims = simItems,
            selectedSubId = subId,
            isLoading = false,
        )
    }

    private fun editableKeyType(isNumeric: Boolean): MmsConfigKeyType {
        return when {
            isNumeric -> MmsConfigKeyType.INT
            else -> MmsConfigKeyType.STRING
        }
    }

    private fun updateEntry(
        key: String,
        keyType: MmsConfigKeyType,
        value: String,
    ) {
        val subId = uiState.value.selectedSubId ?: return

        viewModelScope.launch {
            repository.updateEntry(
                subId = subId,
                key = key,
                keyType = keyType,
                value = value,
            )
            refreshTriggers.trySend(Unit)
        }
    }

    private fun resolveSubId(
        sims: ImmutableList<DebugSim>,
        selected: Int,
    ): SubId? {
        val selectedSubId = SubId(selected).takeIf { selected != UNSET_SUB_ID }
        val isSelectionActive = sims.any { sim -> sim.subId == selectedSubId }

        return when {
            isSelectionActive -> selectedSubId
            else -> sims.firstOrNull()?.subId
        }
    }

    private companion object {
        private const val KEY_SELECTED_SUB_ID = "debug_mms_config_selected_sub_id"
        private const val UNSET_SUB_ID = Int.MIN_VALUE
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5000L
    }
}
