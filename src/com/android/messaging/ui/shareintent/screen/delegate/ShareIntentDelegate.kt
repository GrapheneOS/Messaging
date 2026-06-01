package com.android.messaging.ui.shareintent.screen.delegate

import com.android.messaging.data.shareintent.repository.ShareTargetsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.shareintent.screen.mapper.ShareTargetUiStateMapper
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal interface ShareIntentScreenDelegate {
    val state: Flow<State>
    fun bind(scope: CoroutineScope)
    fun setSearchActive(active: Boolean)
    fun setSearchQuery(query: String)
}

internal class ShareIntentScreenDelegateImpl @Inject constructor(
    private val repository: ShareTargetsRepository,
    private val mapper: ShareTargetUiStateMapper,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ShareIntentScreenDelegate {

    private val targets = MutableStateFlow<ImmutableList<ShareTargetUiState>?>(null)
    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)

    private var isBound = false

    override val state: Flow<State> = combine(
        targets,
        isSearchActive,
        searchQuery,
    ) { allTargets, active, query ->
        when (allTargets) {
            null -> State(isSearchActive = active)

            else -> State(
                isLoading = false,
                targets = filterTargets(allTargets, query),
                isSearchActive = active,
            )
        }
    }.flowOn(defaultDispatcher)

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        scope.launch(defaultDispatcher) {
            repository.observeShareTargets()
                .map(mapper::map)
                .collect { mapped ->
                    targets.value = mapped
                }
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

    private fun filterTargets(
        targets: ImmutableList<ShareTargetUiState>,
        query: String,
    ): ImmutableList<ShareTargetUiState> {
        if (query.isBlank()) {
            return targets
        }

        return targets
            .filter { target ->
                target.matches(query)
            }
            .toImmutableList()
    }

    private fun ShareTargetUiState.matches(query: String): Boolean {
        return displayName.contains(query, ignoreCase = true) ||
            details?.contains(query, ignoreCase = true) == true
    }
}
