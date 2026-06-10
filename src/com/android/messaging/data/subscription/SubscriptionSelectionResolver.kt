package com.android.messaging.data.subscription

import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import kotlinx.collections.immutable.ImmutableList

internal fun resolveSelectedSubscription(
    subscriptions: ImmutableList<Subscription>,
    selectedSelfParticipantId: String?,
    defaultSmsSubscriptionId: Int,
): Subscription? {
    subscriptions
        .firstOrNull { subscription ->
            subscription.selfParticipantId == selectedSelfParticipantId
        }
        ?.let { return it }

    val defaultSelection = when {
        defaultSmsSubscriptionId == ParticipantData.DEFAULT_SELF_SUB_ID -> null
        else -> {
            subscriptions.firstOrNull { subscription ->
                subscription.subId == defaultSmsSubscriptionId
            }
        }
    }

    return defaultSelection ?: subscriptions.firstOrNull()
}
