package com.android.messaging.ui.shareintent.screen.delegate

import com.android.messaging.data.contact.repository.ContactsRepository
import com.android.messaging.data.shareintent.model.ShareTargetConversation
import com.android.messaging.data.shareintent.repository.ShareTargetsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.contacts.usecase.IsReadContactsPermissionGranted
import com.android.messaging.ui.shareintent.screen.mapper.ShareContactSectionMapper
import com.android.messaging.ui.shareintent.screen.mapper.ShareContactUiStateMapper
import com.android.messaging.ui.shareintent.screen.mapper.ShareTargetUiStateMapper
import com.android.messaging.ui.shareintent.screen.model.ShareContactSection
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.ui.shareintent.screen.model.ShareTargetsUiState
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface ShareTargetsDelegate {
    val state: Flow<ShareTargetsUiState>
    val selectedIds: StateFlow<ImmutableSet<String>>
    fun bind(scope: CoroutineScope)
    fun setSearchActive(active: Boolean)
    fun setSearchQuery(query: String)
    fun toggleSelection(target: ShareTargetUiState)
    fun clearSelection()
    fun loadMoreRecent()
    fun collapseRecent()
    fun loadMoreContacts()
    fun onContactsPermissionGranted()
    fun currentSelectedTargets(): ImmutableList<ShareTargetUiState>
}

internal class ShareTargetsDelegateImpl @Inject constructor(
    private val repository: ShareTargetsRepository,
    private val contactsRepository: ContactsRepository,
    private val conversationMapper: ShareTargetUiStateMapper,
    private val contactMapper: ShareContactUiStateMapper,
    private val contactSectionMapper: ShareContactSectionMapper,
    private val isReadContactsPermissionGranted: IsReadContactsPermissionGranted,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ShareTargetsDelegate {

    private val recents = MutableStateFlow<ImmutableList<ShareTargetConversation>?>(null)
    private val contactsState = MutableStateFlow(ContactsState())
    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)
    private val visibleRecentLimit = MutableStateFlow(INITIAL_RECENT_TARGET_COUNT)
    private val contactsReloadTrigger = MutableStateFlow(0)
    private val selectedTargetsList = MutableStateFlow<PersistentList<ShareTargetUiState>>(
        persistentListOf(),
    )
    private val mutableSelectedIds = MutableStateFlow<ImmutableSet<String>>(persistentSetOf())

    private var boundScope: CoroutineScope? = null

    override val selectedIds: StateFlow<ImmutableSet<String>> = mutableSelectedIds.asStateFlow()

    override val state: Flow<ShareTargetsUiState> = combine(
        combine(recents, searchQuery, visibleRecentLimit, ::buildRecentTargets),
        contactsState,
        isSearchActive,
        selectedTargetsList,
    ) { recentTargets, contacts, active, selectedTargets ->
        buildState(
            recentTargets = recentTargets,
            contacts = contacts,
            isSearchActive = active,
            selectedTargets = selectedTargets,
        )
    }.flowOn(defaultDispatcher)

    override fun bind(scope: CoroutineScope) {
        if (boundScope != null) return
        boundScope = scope

        scope.launch(defaultDispatcher) {
            repository.observeShareTargets()
                .collect { conversations ->
                    recents.value = conversations
                    pruneSelection(conversations)
                }
        }

        scope.launch(defaultDispatcher) {
            combine(searchQuery, contactsReloadTrigger) { query, _ -> query }
                .collectLatest(::loadInitialContacts)
        }
    }

    override fun setSearchActive(active: Boolean) {
        isSearchActive.value = active

        if (!active) {
            searchQuery.value = ""
        }
    }

    override fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    override fun toggleSelection(target: ShareTargetUiState) {
        val current = selectedTargetsList.value
        val next = when {
            target.selectionId in mutableSelectedIds.value -> {
                current.removeAll { it.selectionId == target.selectionId }
            }

            else -> current.add(target)
        }

        setSelection(next)
    }

    override fun clearSelection() {
        setSelection(persistentListOf())
    }

    override fun loadMoreRecent() {
        visibleRecentLimit.update { it + RECENT_TARGET_LOAD_MORE_COUNT }
    }

    override fun collapseRecent() {
        visibleRecentLimit.value = INITIAL_RECENT_TARGET_COUNT
    }

    override fun loadMoreContacts() {
        boundScope?.launch(defaultDispatcher) {
            loadMoreContactsPage()
        }
    }

    override fun onContactsPermissionGranted() {
        if (!contactsState.value.hasPermission) {
            contactsReloadTrigger.update { it + 1 }
        }
    }

    override fun currentSelectedTargets(): ImmutableList<ShareTargetUiState> {
        return selectedTargetsList.value
    }

    private fun buildState(
        recentTargets: RecentTargetsState,
        contacts: ContactsState,
        isSearchActive: Boolean,
        selectedTargets: PersistentList<ShareTargetUiState>,
    ): ShareTargetsUiState {
        if (recentTargets.isLoading) {
            return ShareTargetsUiState(
                isLoading = true,
                isSearchActive = isSearchActive,
                hasContactsPermission = contacts.hasPermission,
                selectedIds = mutableSelectedIds.value,
                selectedTargets = selectedTargets,
            )
        }

        return ShareTargetsUiState(
            isLoading = false,
            recentTargets = recentTargets.targets,
            contactSections = contacts.sections,
            canLoadMoreRecent = recentTargets.canLoadMore,
            canCollapseRecent = recentTargets.canCollapse,
            hasContactsPermission = contacts.hasPermission,
            canLoadMoreContacts = contacts.canLoadMore,
            isSearchActive = isSearchActive,
            selectedIds = mutableSelectedIds.value,
            selectedTargets = selectedTargets,
        )
    }

    private fun buildRecentTargets(
        recentConversations: ImmutableList<ShareTargetConversation>?,
        query: String,
        visibleLimit: Int,
    ): RecentTargetsState {
        if (recentConversations == null) {
            return RecentTargetsState(isLoading = true)
        }

        return when {
            query.isNotBlank() -> {
                RecentTargetsState(
                    targets = conversationMapper.map(recentConversations).filterByQuery(query),
                )
            }

            else -> {
                val visibleConversations = recentConversations
                    .take(visibleLimit)
                    .toImmutableList()

                RecentTargetsState(
                    targets = conversationMapper.map(visibleConversations),
                    canLoadMore = visibleConversations.size < recentConversations.size,
                    canCollapse = visibleLimit > INITIAL_RECENT_TARGET_COUNT,
                )
            }
        }
    }

    private suspend fun loadInitialContacts(query: String) {
        if (!isReadContactsPermissionGranted()) {
            contactsState.value = ContactsState(hasPermission = false)
            return
        }

        delay(SEARCH_DEBOUNCE)

        val page = contactsRepository.searchContacts(
            query = query,
            offset = 0
        ).first()

        val contacts = contactMapper.map(page.contacts)

        contactsState.value = ContactsState(
            contacts = contacts,
            sections = contactSectionMapper.map(contacts),
            hasPermission = true,
            canLoadMore = page.nextOffset != null,
            nextOffset = page.nextOffset,
            loadedQuery = query,
        )
    }

    private suspend fun loadMoreContactsPage() {
        val current = contactsState.value
        val nextOffset = current.nextOffset

        if (!current.hasPermission || current.isLoadingMore || nextOffset == null) {
            return
        }

        contactsState.update { it.copy(isLoadingMore = true) }

        val page = contactsRepository
            .searchContacts(
                query = current.loadedQuery,
                offset = nextOffset
            )
            .first()

        if (searchQuery.value != current.loadedQuery) {
            contactsState.update { it.copy(isLoadingMore = false) }
            return
        }

        val additional = contactMapper.map(page.contacts)

        contactsState.update { state ->
            val mergedContacts = mergeContacts(state.contacts, additional)

            state.copy(
                contacts = mergedContacts,
                sections = contactSectionMapper.map(mergedContacts),
                canLoadMore = page.nextOffset != null,
                nextOffset = page.nextOffset,
                isLoadingMore = false,
            )
        }
    }

    private fun mergeContacts(
        existing: ImmutableList<ShareTargetUiState.Contact>,
        additional: ImmutableList<ShareTargetUiState.Contact>,
    ): ImmutableList<ShareTargetUiState.Contact> {
        val seenKeys = existing.mapTo(HashSet()) { it.key }

        return (existing + additional.filter { seenKeys.add(it.key) })
            .toImmutableList()
    }

    private fun setSelection(targets: PersistentList<ShareTargetUiState>) {
        mutableSelectedIds.value = targets.map { it.selectionId }.toPersistentSet()
        selectedTargetsList.value = targets
    }

    private fun pruneSelection(availableRecents: ImmutableList<ShareTargetConversation>) {
        if (selectedTargetsList.value.isEmpty()) {
            return
        }

        val availableConversationIds = availableRecents.mapTo(HashSet()) { it.conversationId }

        val retained = selectedTargetsList.value.filterNot { target ->
            target is ShareTargetUiState.Conversation &&
                target.conversationId !in availableConversationIds
        }

        if (retained.size != selectedTargetsList.value.size) {
            setSelection(retained.toPersistentList())
        }
    }

    private fun ImmutableList<ShareTargetUiState>.filterByQuery(
        query: String,
    ): ImmutableList<ShareTargetUiState> {
        if (query.isBlank()) {
            return this
        }

        return filter { target ->
            target.matches(query)
        }.toImmutableList()
    }

    private fun ShareTargetUiState.matches(query: String): Boolean {
        return displayName.contains(query, ignoreCase = true) ||
            details?.contains(query, ignoreCase = true) == true
    }

    private data class RecentTargetsState(
        val targets: ImmutableList<ShareTargetUiState> = persistentListOf(),
        val canLoadMore: Boolean = false,
        val canCollapse: Boolean = false,
        val isLoading: Boolean = false,
    )

    private data class ContactsState(
        val contacts: ImmutableList<ShareTargetUiState.Contact> = persistentListOf(),
        val sections: ImmutableList<ShareContactSection> = persistentListOf(),
        val hasPermission: Boolean = true,
        val canLoadMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val nextOffset: Int? = null,
        val loadedQuery: String = "",
    )

    private companion object {
        private const val INITIAL_RECENT_TARGET_COUNT = 5
        private const val RECENT_TARGET_LOAD_MORE_COUNT = 15
        private val SEARCH_DEBOUNCE = 150L.milliseconds
    }
}
