package com.android.messaging.ui.conversation.composer.model

import com.android.messaging.data.satellite.model.SatelliteConnection
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.data.subscription.model.Subscription
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

internal data class ConversationSubscriptionSelectionState(
    val subscriptions: ImmutableList<Subscription>,
    val areSubscriptionsLoaded: Boolean,
    val defaultSmsSubscriptionId: SubId,
    val satelliteConnections: ImmutableMap<SubId, SatelliteConnection> = persistentMapOf(),
)
