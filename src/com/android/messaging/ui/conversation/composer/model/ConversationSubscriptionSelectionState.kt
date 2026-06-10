package com.android.messaging.ui.conversation.composer.model

import com.android.messaging.data.subscription.model.Subscription
import kotlinx.collections.immutable.ImmutableList

internal data class ConversationSubscriptionSelectionState(
    val subscriptions: ImmutableList<Subscription>,
    val areSubscriptionsLoaded: Boolean,
    val defaultSmsSubscriptionId: Int,
)
