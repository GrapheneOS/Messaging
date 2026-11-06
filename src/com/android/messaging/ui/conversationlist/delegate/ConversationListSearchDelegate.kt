package com.android.messaging.ui.conversationlist.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversationlist.matcher.ConversationSearchMatcher
import com.android.messaging.data.conversationlist.repository.ConversationListSearchRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversationlist.mapper.ConversationListSearchUiStateMapper
import com.android.messaging.ui.conversationlist.model.ConversationListSearchUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

internal interface ConversationListSearchDelegate {
    val state: StateFlow<ConversationListSearchUiState>

    fun bind(scope: CoroutineScope)
    fun open()
    fun close()
    fun setQuery(query: String)
}

internal class ConversationListSearchDelegateImpl @Inject constructor(
    private val repository: ConversationListSearchRepository,
    private val matcher: ConversationSearchMatcher,
    private val uiStateMapper: ConversationListSearchUiStateMapper,
    private val savedStateHandle: SavedStateHandle,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationListSearchDelegate {

    private val isActive = MutableStateFlow(
        value = savedStateHandle.get<Boolean>(SEARCH_ACTIVE_KEY) == true,
    )
    private val query = MutableStateFlow(
        value = savedStateHandle.get<String>(SEARCH_QUERY_KEY).orEmpty(),
    )

    private val _state = MutableStateFlow(
        value = ConversationListSearchUiState(isActive = isActive.value),
    )
    override val state: StateFlow<ConversationListSearchUiState> = _state.asStateFlow()

    private var isBound = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun bind(scope: CoroutineScope) {
        if (isBound) {
            return
        }

        isBound = true

        isActive
            .flatMapLatest { active ->
                when {
                    active -> observeActiveSearch()
                    else -> flowOf(ConversationListSearchUiState())
                }
            }
            .flowOn(defaultDispatcher)
            .onEach { searchState ->
                // open() and close() swap the state themselves, so a result still in flight
                // from before the swap must not undo it
                if (searchState.isActive == isActive.value) {
                    _state.value = searchState
                }
            }
            .launchIn(scope)
    }

    override fun open() {
        if (isActive.value) {
            return
        }

        // Swap the top bar right away instead of after the first results arrive
        _state.value = ConversationListSearchUiState(isActive = true)
        setActive(isActive = true)
    }

    override fun close() {
        if (!isActive.value) {
            return
        }

        _state.value = ConversationListSearchUiState()
        setQuery(query = "")
        setActive(isActive = false)
    }

    override fun setQuery(query: String) {
        this.query.value = query
        savedStateHandle[SEARCH_QUERY_KEY] = query
    }

    private fun setActive(isActive: Boolean) {
        this.isActive.value = isActive
        savedStateHandle[SEARCH_ACTIVE_KEY] = isActive
    }

    private fun observeActiveSearch(): Flow<ConversationListSearchUiState> {
        return combine(
            repository.observeSearchableConversations().map(matcher::index),
            debouncedQuery(),
        ) { searchIndex, currentQuery ->
            ConversationListSearchUiState(
                isActive = true,
                content = uiStateMapper.map(
                    query = currentQuery,
                    conversationMatches = searchIndex.match(currentQuery),
                ),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun debouncedQuery(): Flow<String> {
        return query.mapLatest { currentQuery ->
            // A cleared query empties the results at once; only typing waits for a pause
            if (currentQuery.isNotBlank()) {
                delay(SEARCH_DEBOUNCE_MILLIS)
            }

            currentQuery
        }
    }

    private companion object {
        private const val SEARCH_ACTIVE_KEY = "conversation_list_search_active"
        private const val SEARCH_QUERY_KEY = "conversation_list_search_query"
        private const val SEARCH_DEBOUNCE_MILLIS = 150L
    }
}
