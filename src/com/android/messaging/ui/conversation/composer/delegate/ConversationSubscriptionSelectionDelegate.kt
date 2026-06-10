package com.android.messaging.ui.conversation.composer.delegate

import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.composer.model.ConversationSubscriptionSelectionState
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal interface ConversationSubscriptionSelectionDelegate {
    val state: StateFlow<ConversationSubscriptionSelectionState>

    fun bind(scope: CoroutineScope)
}

internal class ConversationSubscriptionSelectionDelegateImpl @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
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

    override fun bind(scope: CoroutineScope) {
        if (isBound) {
            return
        }

        isBound = true

        scope.launch(defaultDispatcher) {
            combine(
                subscriptionsRepository.observeActiveSubscriptions(),
                subscriptionsRepository.observeDefaultSmsSubscriptionId(),
            ) { subscriptions, defaultSmsSubscriptionId ->
                ConversationSubscriptionSelectionState(
                    subscriptions = subscriptions,
                    areSubscriptionsLoaded = true,
                    defaultSmsSubscriptionId = defaultSmsSubscriptionId,
                )
            }.collect { selectionState ->
                _state.value = selectionState
            }
        }
    }
}
