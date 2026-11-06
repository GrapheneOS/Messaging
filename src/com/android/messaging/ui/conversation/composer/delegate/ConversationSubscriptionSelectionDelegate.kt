package com.android.messaging.ui.conversation.composer.delegate

import com.android.messaging.data.satellite.repository.SatelliteConnectionsRepository
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.composer.model.ConversationSubscriptionSelectionState
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

internal interface ConversationSubscriptionSelectionDelegate {
    val state: StateFlow<ConversationSubscriptionSelectionState>

    fun bind(scope: CoroutineScope)
}

internal class ConversationSubscriptionSelectionDelegateImpl @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
    private val satelliteConnectionsRepository: SatelliteConnectionsRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationSubscriptionSelectionDelegate {

    private val _state = MutableStateFlow(
        ConversationSubscriptionSelectionState(
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = false,
            defaultSmsSubscriptionId = subscriptionsRepository.getDefaultSmsSubscriptionId(),
        ),
    )

    override val state = _state.asStateFlow()

    private var isBound = false

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun bind(scope: CoroutineScope) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch(defaultDispatcher) {
            val subscriptions = subscriptionsRepository
                .observeActiveSubscriptions()
                .shareIn(scope = this, started = SharingStarted.Lazily, replay = 1)

            val satelliteConnections = subscriptions
                .map { activeSubscriptions ->
                    activeSubscriptions.mapTo(mutableSetOf()) { it.subId }
                }
                .distinctUntilChanged()
                .flatMapLatest { subIds ->
                    satelliteConnectionsRepository.observeSatelliteConnections(subIds = subIds)
                }

            combine(
                subscriptions,
                subscriptionsRepository.observeDefaultSmsSubscriptionId(),
                satelliteConnections,
            ) { activeSubscriptions, defaultSmsSubscriptionId, connections ->
                ConversationSubscriptionSelectionState(
                    subscriptions = activeSubscriptions,
                    areSubscriptionsLoaded = true,
                    defaultSmsSubscriptionId = defaultSmsSubscriptionId,
                    satelliteConnections = connections,
                )
            }.collect { selectionState ->
                _state.value = selectionState
            }
        }
    }
}
