package com.android.messaging.ui.conversation.recipientpicker.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.contact.model.Contact
import com.android.messaging.data.contact.model.ContactDestination
import com.android.messaging.data.contact.model.ContactsPage
import com.android.messaging.data.contact.repository.ContactsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
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
    private val contactDestinationFormatter: ContactDestinationFormatter,
    private val contactsRepository: ContactsRepository,
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

    private var searchSession = ContactSearchSession(
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
        val canonicalDestinations = destinations
            .asSequence()
            .map { contactDestinationFormatter.canonicalize(value = it) }
            .filter { it.isNotEmpty() }
            .toSet()

        excludedDestinationsFlow.value = canonicalDestinations
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

    private fun mergeContacts(
        existingContacts: List<Contact>,
        additionalContacts: List<Contact>,
    ): ImmutableList<Contact> {
        val seenContactIds = LinkedHashSet<Long>()

        return (existingContacts + additionalContacts)
            .asSequence()
            .filter { contact -> seenContactIds.add(contact.id) }
            .toImmutableList()
    }

    private suspend fun startSearch(searchInputs: SearchInputs) {
        applySearchStartedState()
        delay(searchDebounce)

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
        val visibleItems = buildVisibleItems(
            query = query,
            contacts = persistentListOf(),
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
                items = visibleItems,
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
        val requestedPage = loadContactsPage(
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

        val defaultPage = loadContactsPage(
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
        page: ContactsPage,
    ): Boolean {
        return query.isBlank() || page.contacts.isNotEmpty()
    }

    private suspend fun loadContactsPage(
        query: String,
        offset: Int,
        excludedDestinations: Set<String>,
    ): ContactsPage {
        var nextOffset: Int? = offset
        val visibleContacts = mutableListOf<Contact>()

        while (nextOffset != null) {
            val rawPage = contactsRepository
                .searchContacts(
                    query = query,
                    offset = nextOffset,
                )
                .first()

            rawPage.contacts.forEach { contact ->
                val filtered = filterExcludedDestinations(
                    contact = contact,
                    excludedDestinations = excludedDestinations,
                )

                if (filtered != null) {
                    visibleContacts.add(filtered)
                }
            }

            if (visibleContacts.isNotEmpty() || rawPage.nextOffset == null) {
                return ContactsPage(
                    contacts = visibleContacts.toImmutableList(),
                    nextOffset = rawPage.nextOffset,
                )
            }

            nextOffset = rawPage.nextOffset
        }

        return ContactsPage(
            contacts = persistentListOf(),
            nextOffset = null,
        )
    }

    private fun filterExcludedDestinations(
        contact: Contact,
        excludedDestinations: Set<String>,
    ): Contact? {
        if (excludedDestinations.isEmpty()) {
            return contact
        }

        val remainingDestinations = contact.destinations
            .filterNot { destination -> destination.normalizedValue in excludedDestinations }
            .toPersistentList()

        return when {
            remainingDestinations.isEmpty() -> null
            remainingDestinations.size == contact.destinations.size -> contact
            else -> {
                contact.copy(destinations = remainingDestinations)
            }
        }
    }

    private fun applyInitialSearchResult(result: InitialSearchResult) {
        _state.update { currentState ->
            currentState.copy(
                items = buildVisibleItems(
                    query = currentState.query,
                    contacts = result.page.contacts,
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

        val nextPage = loadContactsPage(
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

    private fun applyLoadMoreResult(page: ContactsPage) {
        _state.update { currentState ->
            val mergedContacts = mergeContacts(
                existingContacts = currentState.items.mapNotNull { item ->
                    when (item) {
                        is RecipientPickerListItem.Contact -> item.contact
                        is RecipientPickerListItem.SyntheticPhone -> null
                    }
                },
                additionalContacts = page.contacts,
            )

            val visibleItems = buildVisibleItems(
                query = currentState.query,
                contacts = mergedContacts,
                excludedDestinations = excludedDestinationsFlow.value,
            )

            currentState.copy(
                items = visibleItems,
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
        transform: (ContactSearchSession) -> ContactSearchSession,
    ) {
        searchSessionMutex.withLock {
            searchSession = transform(searchSession)
        }
    }

    private fun buildVisibleItems(
        query: String,
        contacts: List<Contact>,
        excludedDestinations: Set<String>,
    ): ImmutableList<RecipientPickerListItem> {
        val syntheticItem = createSyntheticItemOrNull(
            query = query,
            contacts = contacts,
            excludedDestinations = excludedDestinations,
        )

        val contactItems = contacts
            .map(RecipientPickerListItem::Contact)
            .toImmutableList()

        return when {
            syntheticItem == null -> contactItems
            else -> {
                persistentListOf<RecipientPickerListItem>(syntheticItem)
                    .addAll(contactItems)
            }
        }
    }

    private fun createSyntheticItemOrNull(
        query: String,
        contacts: List<Contact>,
        excludedDestinations: Set<String>,
    ): RecipientPickerListItem.SyntheticPhone? {
        val candidate = createSyntheticCandidateOrNull(query = query) ?: return null

        val isAlreadyAContactDestination = contacts.any { contact ->
            contact.destinations.any { destination ->
                candidate.matchesDestination(destination = destination)
            }
        }

        return when {
            candidate.isExcludedBy(excludedDestinations) -> null
            isAlreadyAContactDestination -> null
            else -> candidate.toListItem()
        }
    }

    private fun createSyntheticCandidateOrNull(
        query: String,
    ): SyntheticCandidate? {
        val trimmedQuery = query.trim()

        return when {
            trimmedQuery.isEmpty() -> null
            !PhoneUtils.isValidSmsMmsDestination(trimmedQuery) -> null
            else -> {
                SyntheticCandidate(
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

    private fun SyntheticCandidate.matchesDestination(
        destination: ContactDestination,
    ): Boolean {
        return destinationIdentity.matches(
            other = createDestinationIdentity(rawDestination = destination.value),
        )
    }

    private fun normalizeDestination(rawDestination: String): String {
        return contactDestinationFormatter.canonicalize(value = rawDestination)
    }

    private data class InitialSearchResult(
        val effectiveQuery: String,
        val page: ContactsPage,
    )

    private data class LoadMoreRequest(
        val effectiveQuery: String,
        val inputQuery: String,
        val excludedDestinations: Set<String>,
        val offset: Int,
    )

    private data class ContactSearchSession(
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

    private data class SyntheticCandidate(
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
        private val searchDebounce = 150L.milliseconds
        private const val SEARCH_QUERY_KEY = "search_query"
        private const val SYNTHETIC_RECIPIENT_ID_PREFIX = "synthetic:"
    }
}
