package com.android.messaging.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.data.search.repository.MessageSearchRepository
import com.android.messaging.ui.conversation.entry.ConversationLaunchStore
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
internal class SearchViewModel @Inject constructor(
    private val messageSearchRepository: MessageSearchRepository,
    private val conversationLaunchStore: ConversationLaunchStore,
) : ViewModel() {

    internal sealed interface UiState {
        data object Idle : UiState
        data object Searching : UiState
        data class Results(
            val results: ImmutableList<MessageSearchResult>,
        ) : UiState
    }

    private val queryFlow = MutableStateFlow("")

    val uiState: StateFlow<UiState> = queryFlow
        .debounce(timeoutMillis = SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .flatMapLatest(::search)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
            ),
            initialValue = UiState.Idle,
        )

    private val _navigationEvents = MutableSharedFlow<ConversationId>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun onQueryChanged(query: String) {
        queryFlow.value = query
    }

    fun onResultClicked(result: MessageSearchResult) {
        // Ask the conversation screen to scroll to the matching message once it opens.
        conversationLaunchStore.submit(
            request = ConversationEntryLaunchRequest(
                conversationId = result.conversationId,
                messageId = result.messageId,
            ),
        )
        _navigationEvents.tryEmit(result.conversationId)
    }

    private fun search(query: String): Flow<UiState> {
        return when {
            query.isBlank() -> flowOf(UiState.Idle)

            else -> {
                messageSearchRepository
                    .search(query = query)
                    .map<ImmutableList<MessageSearchResult>, UiState>(UiState::Results)
                    .onStart { emit(UiState.Searching) }
            }
        }
    }

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 200L
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
