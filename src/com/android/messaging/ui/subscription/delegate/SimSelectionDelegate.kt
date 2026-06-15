package com.android.messaging.ui.subscription.delegate

import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.data.subscription.resolveSelectedSubscription
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.subscription.model.SimSelectionUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal interface SimSelectionDelegate {
    val state: StateFlow<SimSelectionUiState>
    fun bind(scope: CoroutineScope)
    fun select(selfParticipantId: String)
    fun currentSelectedSelfParticipantId(): String?
}

internal class SimSelectionDelegateImpl @Inject constructor(
    private val subscriptionsRepository: SubscriptionsRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : SimSelectionDelegate {

    private val requestedSelfParticipantId = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(SimSelectionUiState())
    override val state: StateFlow<SimSelectionUiState> = _state.asStateFlow()

    private var isBound = false

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        scope.launch(defaultDispatcher) {
            combine(
                subscriptionsRepository.observeActiveSubscriptions(),
                subscriptionsRepository.observeDefaultSmsSubscriptionId(),
                requestedSelfParticipantId,
            ) { subscriptions, defaultSmsSubscriptionId, requestedId ->
                val selectedSubscription = resolveSelectedSubscription(
                    subscriptions = subscriptions,
                    selectedSelfParticipantId = requestedId,
                    defaultSmsSubscriptionId = defaultSmsSubscriptionId,
                )

                SimSelectionUiState(
                    subscriptions = subscriptions,
                    selectedSelfParticipantId = selectedSubscription?.selfParticipantId,
                )
            }.collect { selectionState ->
                _state.value = selectionState
            }
        }
    }

    override fun select(selfParticipantId: String) {
        requestedSelfParticipantId.value = selfParticipantId
    }

    override fun currentSelectedSelfParticipantId(): String? {
        val currentState = _state.value
        val selectedSubscription = currentState.subscriptions
            .firstOrNull { subscription ->
                subscription.selfParticipantId == currentState.selectedSelfParticipantId
            }
            ?: return null

        // Only bind a real, sendable subscription. Debug-emulated SIMs carry a placeholder
        // self participant id that is not backed by a participant row, so binding it would
        // break message sending; fall back to the conversation's own self instead.
        return when (selectedSubscription.subId) {
            ParticipantData.DEFAULT_SELF_SUB_ID -> null
            else -> selectedSubscription.selfParticipantId
        }
    }
}
