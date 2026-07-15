package com.android.messaging.ui.vcarddetail.screen

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.R
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.data.vcarddetail.repository.VCardDetailRepository
import com.android.messaging.domain.vcarddetail.model.AddVCardToContactsResult
import com.android.messaging.domain.vcarddetail.usecase.AddVCardToContacts
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.vcarddetail.screen.mapper.VCardDetailUiStateMapper
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailAction as Action
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailScreenEffect as Effect
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailUiState as State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface VCardDetailScreenModel {
    val effects: Flow<Effect>
    val uiState: StateFlow<State>

    fun onAction(action: Action)
    fun refresh()
}

@HiltViewModel
internal class VCardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VCardDetailRepository,
    private val uiStateMapper: VCardDetailUiStateMapper,
    private val addVCardToContacts: AddVCardToContacts,
) : ViewModel(),
    VCardDetailScreenModel {

    private val vCardUri: String = savedStateHandle
        .get<Uri>(UIIntents.UI_INTENT_EXTRA_VCARD_URI)
        ?.toString()
        .orEmpty()

    private val _effects = Channel<Effect>(capacity = Channel.BUFFERED)
    override val effects: Flow<Effect> = _effects.receiveAsFlow()

    private val _uiState = MutableStateFlow(State())
    override val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val refreshTriggers = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var addToContactsName: String? = null
    private var scratchUri: String? = null

    init {
        viewModelScope.launch {
            repository
                .observeVCard(
                    vCardUri = vCardUri,
                    refreshes = refreshTriggers,
                )
                .collect(::handleResult)
        }
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.FieldClicked -> handleFieldClicked(action.action)
            is Action.FieldLongClicked -> emitEffect(Effect.CopyToClipboard(action.value))
            is Action.AddToContactsClicked -> handleAddToContactsClicked()
        }
    }

    override fun refresh() {
        refreshTriggers.tryEmit(Unit)
    }

    private fun handleResult(result: VCardDetailResult) {
        if (result == VCardDetailResult.Loading && uiState.value.contacts.isNotEmpty()) {
            return
        }

        if (result is VCardDetailResult.Loaded) {
            addToContactsName = result.contacts.singleOrNull()?.displayName
        }

        _uiState.value = uiStateMapper.map(result)

        if (result is VCardDetailResult.Failed) {
            emitEffect(Effect.ShowMessage(R.string.failed_loading_vcard))
            emitEffect(Effect.Close)
        }
    }

    private fun handleFieldClicked(action: VCardFieldAction) {
        if (action is VCardFieldAction.None) {
            return
        }

        emitEffect(Effect.OpenFieldAction(action))
    }

    private fun handleAddToContactsClicked() {
        val preparedUri = scratchUri
        if (preparedUri != null) {
            emitEffect(Effect.LaunchSaveToContacts(preparedUri))
            return
        }

        viewModelScope.launch {
            when (val result = addVCardToContacts(vCardUri, addToContactsName)) {
                is AddVCardToContactsResult.Prepared -> {
                    scratchUri = result.scratchUri
                    emitEffect(Effect.LaunchSaveToContacts(result.scratchUri))
                }

                AddVCardToContactsResult.Failed -> {
                    emitEffect(Effect.ShowMessage(R.string.failed_saving_vcard_to_contacts))
                }
            }
        }
    }

    private fun emitEffect(effect: Effect) {
        _effects.trySend(effect)
    }
}
