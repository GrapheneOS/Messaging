package com.android.messaging.ui.subscription.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.subscription.model.Subscription
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SimSelectionUiState(
    val subscriptions: ImmutableList<Subscription> = persistentListOf(),
    val selectedSelfParticipantId: String? = null,
)
