package com.android.messaging.ui.conversation.v2.recipientpicker.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.data.conversation.repository.ConversationRecipientsPage
import com.android.messaging.data.conversation.repository.ConversationRecipientsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerListItem
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface RecipientPickerDelegate {
    val state: StateFlow<RecipientPickerUiState>

    fun bind(scope: CoroutineScope)

    fun onLoadMore()

    fun onExcludedDestinationsChanged(destinations: Set<String>)

    fun onQueryChanged(query: String)
}

internal class RecipientPickerDelegateImpl @Inject constructor(
    private val conversationRecipientsRepository: ConversationRecipientsRepository,
    private val isReadContactsPermissionGranted: IsReadContactsPermissionGranted,
    private val savedStateHandle: SavedStateHandle,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : RecipientPickerDelegate {

    private val queryFlow = MutableStateFlow(
        value = savedStateHandle.get<String>(SEARCH_QUERY_KEY).orEmpty(),
    )
    private val excludedDestinationsFlow = MutableStateFlow<Set<String>>(
        value = emptySet(),
    )

    private val _state = MutableStateFlow(
        value = RecipientPickerUiState(
            query = queryFlow.value,
            isLoading = false,
        ),
    )

    override val state = _state.asStateFlow()

    private var boundScope: CoroutineScope? = null

    private val phoneUtils by lazy { PhoneUtils.getDefault() }

    private var searchSession = RecipientSearchSession(
        effectiveQuery = queryFlow.value,
        hasCompletedInitialLoad = false,
        nextPageOffset = null,
    )

    private val searchSessionMutex = Mutex()

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch(defaultDispatcher) {
            combine(
                queryFlow,
                excludedDestinationsFlow,
            ) { query, excludedDestinations ->
                SearchInputs(
                    query = query,
                    excludedDestinations = excludedDestinations,
                )
            }.collectLatest { searchInputs ->
                handleSearchInputsChanged(searchInputs = searchInputs)
            }
        }
    }

    override fun onLoadMore() {
        boundScope?.launch(defaultDispatcher) {
            val loadMoreRequest = createLoadMoreRequest() ?: return@launch
            loadMore(request = loadMoreRequest)
        }
    }

    override fun onExcludedDestinationsChanged(destinations: Set<String>) {
        val normalizedDestinations = destinations
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        excludedDestinationsFlow.value = normalizedDestinations
    }

    override fun onQueryChanged(query: String) {
        updateQueryInState(query = query)

        if (query != queryFlow.value) {
            queryFlow.value = query
            savedStateHandle[SEARCH_QUERY_KEY] = query
        }
    }

    private suspend fun handleSearchInputsChanged(searchInputs: SearchInputs) {
        if (!isReadContactsPermissionGranted()) {
            applyPermissionDeniedState(query = searchInputs.query)
            return
        }

        startSearch(searchInputs = searchInputs)
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

    private suspend fun startSearch(searchInputs: SearchInputs) {
        applySearchStartedState()
        delay(timeMillis = SEARCH_DEBOUNCE_MILLIS)

        val initialSearchResult = resolveInitialSearch(searchInputs = searchInputs)
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
        val visibleRecipients = buildVisibleRecipients(
            query = query,
            recipients = persistentListOf(),
            excludedDestinations = excludedDestinationsFlow.value,
        )

        updateSearchSession { currentSearchSession ->
            currentSearchSession.copy(
                effectiveQuery = query,
                nextPageOffset = null,
            )
        }

        _state.update { currentState ->
            currentState.copy(
                canLoadMore = false,
                items = visibleRecipients,
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

        _state.update { currentState ->
            currentState.copy(
                canLoadMore = false,
                hasContactsPermission = true,
                isLoading = shouldShowInitialLoader,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun resolveInitialSearch(
        searchInputs: SearchInputs,
    ): InitialSearchResult {
        val requestedPage = loadRecipientsPage(
            query = searchInputs.query,
            offset = 0,
            excludedDestinations = searchInputs.excludedDestinations,
        )

        if (shouldUseRequestedPage(query = searchInputs.query, page = requestedPage)) {
            return InitialSearchResult(
                effectiveQuery = searchInputs.query,
                page = requestedPage,
            )
        }

        val defaultPage = loadRecipientsPage(
            query = "",
            offset = 0,
            excludedDestinations = searchInputs.excludedDestinations,
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
        excludedDestinations: Set<String>,
    ): ConversationRecipientsPage {
        var nextOffset: Int? = offset
        val visibleRecipients = mutableListOf<ConversationRecipient>()

        while (nextOffset != null) {
            val rawPage = conversationRecipientsRepository
                .searchRecipients(
                    query = query,
                    offset = nextOffset,
                )
                .first()

            visibleRecipients.addAll(
                rawPage.recipients.filterNot { recipient ->
                    recipient.destination in excludedDestinations
                },
            )

            if (visibleRecipients.isNotEmpty() || rawPage.nextOffset == null) {
                return ConversationRecipientsPage(
                    recipients = visibleRecipients.toImmutableList(),
                    nextOffset = rawPage.nextOffset,
                )
            }

            nextOffset = rawPage.nextOffset
        }

        return ConversationRecipientsPage(
            recipients = persistentListOf(),
            nextOffset = null,
        )
    }

    private fun applyInitialSearchResult(result: InitialSearchResult) {
        _state.update { currentState ->
            currentState.copy(
                items = buildVisibleRecipients(
                    query = currentState.query,
                    recipients = result.page.recipients,
                    excludedDestinations = excludedDestinationsFlow.value,
                ),
                canLoadMore = result.page.nextOffset != null,
                hasContactsPermission = true,
                isLoading = false,
                isLoadingMore = false,
            )
        }
    }

    private suspend fun createLoadMoreRequest(): LoadMoreRequest? {
        val currentState = _state.value

        return when {
            currentState.isLoading || currentState.isLoadingMore -> null
            !currentState.hasContactsPermission -> null

            else -> {
                searchSessionMutex.withLock {
                    val nextPageOffset = searchSession.nextPageOffset ?: return@withLock null

                    LoadMoreRequest(
                        effectiveQuery = searchSession.effectiveQuery,
                        inputQuery = currentState.query,
                        excludedDestinations = excludedDestinationsFlow.value,
                        offset = nextPageOffset,
                    )
                }
            }
        }
    }

    private suspend fun loadMore(request: LoadMoreRequest) {
        applyLoadMoreStartedState()

        val nextPage = loadRecipientsPage(
            query = request.effectiveQuery,
            offset = request.offset,
            excludedDestinations = request.excludedDestinations,
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
        _state.update { currentState ->
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
            _state.value.query == request.inputQuery
    }

    private fun applyLoadMoreStoppedState() {
        _state.update { currentState ->
            currentState.copy(
                isLoadingMore = false,
            )
        }
    }

    private fun applyLoadMoreResult(page: ConversationRecipientsPage) {
        _state.update { currentState ->
            val mergedRecipients = mergeRecipients(
                existingRecipients = currentState.items.mapNotNull { item ->
                    when (item) {
                        is RecipientPickerListItem.Contact -> item.recipient
                        is RecipientPickerListItem.SyntheticPhone -> null
                    }
                },
                additionalRecipients = page.recipients,
            )

            val visibleRecipients = buildVisibleRecipients(
                query = currentState.query,
                recipients = mergedRecipients,
                excludedDestinations = excludedDestinationsFlow.value,
            )

            currentState.copy(
                items = visibleRecipients,
                canLoadMore = page.nextOffset != null,
                isLoadingMore = false,
            )
        }
    }

    private fun updateQueryInState(query: String) {
        _state.update { currentState ->
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

    private fun buildVisibleRecipients(
        query: String,
        recipients: List<ConversationRecipient>,
        excludedDestinations: Set<String>,
    ): ImmutableList<RecipientPickerListItem> {
        val syntheticRecipient = createSyntheticRecipientOrNull(
            query = query,
            recipients = recipients,
            excludedDestinations = excludedDestinations,
        )

        val contactItems = recipients
            .map(RecipientPickerListItem::Contact)
            .toImmutableList()

        if (syntheticRecipient == null) {
            return contactItems
        }

        return persistentListOf<RecipientPickerListItem>(syntheticRecipient)
            .addAll(contactItems)
    }

    private fun createSyntheticRecipientOrNull(
        query: String,
        recipients: List<ConversationRecipient>,
        excludedDestinations: Set<String>,
    ): RecipientPickerListItem.SyntheticPhone? {
        val candidate = createSyntheticRecipientCandidateOrNull(query = query) ?: return null

        return when {
            candidate.isExcludedBy(excludedDestinations) -> null
            recipients.any { recipient -> candidate.matches(recipient) } -> null
            else -> candidate.toListItem()
        }
    }

    private fun createSyntheticRecipientCandidateOrNull(
        query: String,
    ): SyntheticRecipientCandidate? {
        val trimmedQuery = query.trim()

        return when {
            trimmedQuery.isEmpty() -> null
            !PhoneUtils.isValidSmsMmsDestination(trimmedQuery) -> null
            else -> {
                SyntheticRecipientCandidate(
                    rawQuery = trimmedQuery,
                    destinationIdentity = createDestinationIdentity(
                        rawDestination = trimmedQuery,
                    ),
                )
            }
        }
    }

    private fun createDestinationIdentity(rawDestination: String): DestinationIdentity {
        val trimmedDestination = rawDestination.trim()

        return DestinationIdentity(
            rawDestination = trimmedDestination,
            normalizedDestination = normalizeDestination(rawDestination = trimmedDestination),
        )
    }

    private fun SyntheticRecipientCandidate.matches(recipient: ConversationRecipient): Boolean {
        return destinationIdentity.matches(
            other = createDestinationIdentity(rawDestination = recipient.destination),
        )
    }

    private fun normalizeDestination(rawDestination: String): String {
        val trimmedDestination = rawDestination.trim()

        return when {
            trimmedDestination.isEmpty() -> trimmedDestination
            MmsSmsUtils.isEmailAddress(trimmedDestination) -> trimmedDestination
            else -> phoneUtils.getCanonicalForEnteredPhoneNumber(trimmedDestination)
        }
    }

    private data class InitialSearchResult(
        val effectiveQuery: String,
        val page: ConversationRecipientsPage,
    )

    private data class LoadMoreRequest(
        val effectiveQuery: String,
        val inputQuery: String,
        val excludedDestinations: Set<String>,
        val offset: Int,
    )

    private data class RecipientSearchSession(
        val effectiveQuery: String,
        val hasCompletedInitialLoad: Boolean,
        val nextPageOffset: Int?,
    )

    private data class SearchInputs(
        val query: String,
        val excludedDestinations: Set<String>,
    )

    private data class DestinationIdentity(
        val rawDestination: String,
        val normalizedDestination: String,
    ) {
        fun isExcludedBy(excludedDestinations: Set<String>): Boolean {
            return rawDestination in excludedDestinations ||
                normalizedDestination in excludedDestinations
        }

        fun matches(other: DestinationIdentity): Boolean {
            return matches(destination = other.rawDestination) ||
                matches(destination = other.normalizedDestination)
        }

        private fun matches(destination: String): Boolean {
            return destination.isNotEmpty() &&
                (rawDestination == destination || normalizedDestination == destination)
        }
    }

    private data class SyntheticRecipientCandidate(
        val rawQuery: String,
        val destinationIdentity: DestinationIdentity,
    ) {
        fun isExcludedBy(excludedDestinations: Set<String>): Boolean {
            return destinationIdentity.isExcludedBy(excludedDestinations = excludedDestinations)
        }

        fun toListItem(): RecipientPickerListItem.SyntheticPhone {
            return RecipientPickerListItem.SyntheticPhone(
                id = "$SYNTHETIC_RECIPIENT_ID_PREFIX$rawQuery",
                rawQuery = rawQuery,
                destination = rawQuery,
                normalizedDestination = destinationIdentity.normalizedDestination,
            )
        }
    }

    private companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 150L
        private const val SEARCH_QUERY_KEY = "search_query"
        private const val SYNTHETIC_RECIPIENT_ID_PREFIX = "synthetic:"
    }
}
