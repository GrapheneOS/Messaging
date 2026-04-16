package com.android.messaging.ui.conversation.v2.recipientpicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationRecipientsPage
import com.android.messaging.data.conversation.repository.ConversationRecipientsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface RecipientPickerModel {
    val uiState: StateFlow<RecipientPickerUiState>

    fun onLoadMore()

    fun onQueryChanged(query: String)
}

@HiltViewModel
internal class RecipientPickerViewModel @Inject constructor(
    private val conversationRecipientsRepository: ConversationRecipientsRepository,
    private val isReadContactsPermissionGranted: IsReadContactsPermissionGranted,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(),
    RecipientPickerModel {

    private val queryFlow: StateFlow<String> = savedStateHandle.getStateFlow(
        key = SEARCH_QUERY_KEY,
        initialValue = "",
    )

    private val _uiState = MutableStateFlow(
        RecipientPickerUiState(
            query = queryFlow.value,
            isLoading = false,
        ),
    )

    private var searchSession = RecipientSearchSession(
        effectiveQuery = queryFlow.value,
        hasCompletedInitialLoad = false,
        nextPageOffset = null,
    )
    private val searchSessionMutex = Mutex()

    override val uiState = _uiState.asStateFlow()

    init {
        bindQueryFlow()
    }

    private fun bindQueryFlow() {
        viewModelScope.launch(defaultDispatcher) {
            queryFlow.collectLatest { query ->
                handleQueryChanged(query = query)
            }
        }
    }

    private suspend fun handleQueryChanged(query: String) {
        if (!isReadContactsPermissionGranted()) {
            applyPermissionDeniedState(query = query)
            return
        }

        startSearch(query = query)
    }

    override fun onLoadMore() {
        viewModelScope.launch(defaultDispatcher) {
            val loadMoreRequest = createLoadMoreRequest() ?: return@launch
            loadMore(request = loadMoreRequest)
        }
    }

    private fun mergeRecipients(
        existingRecipients: List<ConversationRecipient>,
        additionalRecipients: List<ConversationRecipient>,
    ): ImmutableList<ConversationRecipient> {
        val seenDestinations = LinkedHashSet<String>()

        return (existingRecipients + additionalRecipients)
            .asSequence()
            .filter { recipient ->
                seenDestinations.add(recipient.destination)
            }
            .toImmutableList()
    }

    override fun onQueryChanged(query: String) {
        updateQueryInUiState(query = query)

        if (query != queryFlow.value) {
            savedStateHandle[SEARCH_QUERY_KEY] = query
        }
    }

    private suspend fun startSearch(query: String) {
        applySearchStartedState()
        delay(timeMillis = SEARCH_DEBOUNCE_MILLIS)

        val initialSearchResult = resolveInitialSearch(query = query)
        updateSearchSession { currentSearchSession ->
            currentSearchSession.copy(
                effectiveQuery = initialSearchResult.effectiveQuery,
                hasCompletedInitialLoad = true,
                nextPageOffset = initialSearchResult.page.nextOffset,
            )
        }

        applyInitialSearchResult(result = initialSearchResult)
    }

    private suspend fun applyPermissionDeniedState(query: String) {
        updateSearchSession { currentSearchSession ->
            currentSearchSession.copy(
                effectiveQuery = query,
                nextPageOffset = null,
            )
        }

        _uiState.update { currentState ->
            currentState.copy(
                canLoadMore = false,
                contacts = persistentListOf(),
                hasContactsPermission = false,
                isLoading = false,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun applySearchStartedState() {
        val shouldShowInitialLoader = searchSessionMutex.withLock {
            !searchSession.hasCompletedInitialLoad
        }

        _uiState.update { currentState ->
            currentState.copy(
                canLoadMore = false,
                hasContactsPermission = true,
                isLoading = shouldShowInitialLoader,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun resolveInitialSearch(query: String): InitialSearchResult {
        val requestedPage = loadRecipientsPage(
            query = query,
            offset = 0,
        )

        val shouldUseRequestedPage = shouldUseRequestedPage(
            query = query,
            page = requestedPage,
        )

        if (shouldUseRequestedPage) {
            return InitialSearchResult(
                effectiveQuery = query,
                page = requestedPage,
            )
        }

        val defaultPage = loadRecipientsPage(
            query = "",
            offset = 0,
        )

        return InitialSearchResult(
            effectiveQuery = "",
            page = defaultPage,
        )
    }

    private fun shouldUseRequestedPage(
        query: String,
        page: ConversationRecipientsPage,
    ): Boolean {
        return query.isBlank() || page.recipients.isNotEmpty()
    }

    private suspend fun loadRecipientsPage(
        query: String,
        offset: Int,
    ): ConversationRecipientsPage {
        return conversationRecipientsRepository
            .searchRecipients(
                query = query,
                offset = offset,
            )
            .first()
    }

    private fun applyInitialSearchResult(result: InitialSearchResult) {
        _uiState.update { currentState ->
            currentState.copy(
                contacts = result.page.recipients,
                canLoadMore = result.page.nextOffset != null,
                hasContactsPermission = true,
                isLoading = false,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun createLoadMoreRequest(): LoadMoreRequest? {
        val currentUiState = _uiState.value

        if (currentUiState.isLoading || currentUiState.isLoadingMore) {
            return null
        }

        if (!currentUiState.hasContactsPermission) {
            return null
        }

        return searchSessionMutex.withLock {
            val nextPageOffset = searchSession.nextPageOffset ?: return@withLock null

            LoadMoreRequest(
                effectiveQuery = searchSession.effectiveQuery,
                inputQuery = currentUiState.query,
                offset = nextPageOffset,
            )
        }
    }

    private suspend fun loadMore(request: LoadMoreRequest) {
        applyLoadMoreStartedState()

        val nextPage = loadRecipientsPage(
            query = request.effectiveQuery,
            offset = request.offset,
        )

        if (!isLoadMoreRequestCurrent(request = request)) {
            applyLoadMoreStoppedState()
            return
        }

        updateSearchSession { currentSearchSession ->
            currentSearchSession.copy(
                nextPageOffset = nextPage.nextOffset,
            )
        }

        applyLoadMoreResult(page = nextPage)
    }

    private fun applyLoadMoreStartedState() {
        _uiState.update { currentState ->
            currentState.copy(
                isLoadingMore = true,
            )
        }
    }

    private suspend fun isLoadMoreRequestCurrent(request: LoadMoreRequest): Boolean {
        val currentEffectiveQuery = searchSessionMutex.withLock {
            searchSession.effectiveQuery
        }

        return currentEffectiveQuery == request.effectiveQuery &&
            _uiState.value.query == request.inputQuery
    }

    private fun applyLoadMoreStoppedState() {
        _uiState.update { currentState ->
            currentState.copy(
                isLoadingMore = false,
            )
        }
    }

    private fun applyLoadMoreResult(page: ConversationRecipientsPage) {
        _uiState.update { currentState ->
            currentState.copy(
                contacts = mergeRecipients(
                    existingRecipients = currentState.contacts,
                    additionalRecipients = page.recipients,
                ),
                canLoadMore = page.nextOffset != null,
                isLoadingMore = false,
            )
        }
    }

    private fun updateQueryInUiState(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                query = query,
            )
        }
    }

    private suspend fun updateSearchSession(
        transform: (RecipientSearchSession) -> RecipientSearchSession,
    ) {
        searchSessionMutex.withLock {
            searchSession = transform(searchSession)
        }
    }

    private data class InitialSearchResult(
        val effectiveQuery: String,
        val page: ConversationRecipientsPage,
    )

    private data class LoadMoreRequest(
        val effectiveQuery: String,
        val inputQuery: String,
        val offset: Int,
    )

    private data class RecipientSearchSession(
        val effectiveQuery: String,
        val hasCompletedInitialLoad: Boolean,
        val nextPageOffset: Int?,
    )

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 150L
        private const val SEARCH_QUERY_KEY = "search_query"
    }
}
