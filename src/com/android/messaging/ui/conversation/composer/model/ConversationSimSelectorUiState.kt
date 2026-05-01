package com.android.messaging.ui.conversation.composer.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.metadata.ConversationSubscription
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ConversationSimSelectorUiState(
    val subscriptions: ImmutableList<ConversationSubscription> = persistentListOf(),
    val selectedSubscription: ConversationSubscription? = null,
) {
    val isAvailable: Boolean
        get() = subscriptions.size > 1
}
