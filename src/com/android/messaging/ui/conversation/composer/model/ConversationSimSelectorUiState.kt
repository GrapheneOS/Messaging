package com.android.messaging.ui.conversation.composer.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.subscription.model.Subscription
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationSimSelectorUiState(
    val subscriptions: ImmutableList<Subscription> = persistentListOf(),
    val selectedSubscription: Subscription? = null,
    val isLoading: Boolean = false,
) {
    val isAvailable: Boolean
        get() = !isLoading && subscriptions.size > 1
}
