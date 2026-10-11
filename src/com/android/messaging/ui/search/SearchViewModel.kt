package com.android.messaging.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.search.model.MessageSearchResult
import com.android.messaging.data.search.repository.MessageSearchRepository
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
internal class SearchViewModel @Inject constructor(
    private val messageSearchRepository: MessageSearchRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val query: StateFlow<String> = queryFlow.asStateFlow()

    val results: StateFlow<ImmutableList<MessageSearchResult>> = queryFlow
        .debounce(timeoutMillis = SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .flatMapLatest(::search)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STATEFLOW_STOP_TIMEOUT_MILLIS,
            ),
            initialValue = persistentListOf(),
        )

    private val _navigationEvents = MutableSharedFlow<ConversationId>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun onQueryChanged(query: String) {
        queryFlow.value = query
    }

    fun onResultClicked(conversationId: ConversationId) {
        _navigationEvents.tryEmit(conversationId)
    }

    private fun search(query: String): Flow<ImmutableList<MessageSearchResult>> {
        return if (query.isBlank()) {
            flowOf(persistentListOf())
        } else {
            messageSearchRepository.search(query = query)
        }
    }

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 200L
        private const val STATEFLOW_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
