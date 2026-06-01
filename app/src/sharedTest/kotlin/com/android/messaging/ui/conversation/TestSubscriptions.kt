package com.android.messaging.ui.conversation

import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription

internal const val TEST_ATT_SUBSCRIPTION_NAME = "AT&T Business"
internal const val TEST_VERIZON_SUBSCRIPTION_NAME = "Verizon"

internal val testAttSubscription = Subscription(
    selfParticipantId = "self-2",
    subId = 2,
    label = ConversationSubscriptionLabel.Named(name = TEST_ATT_SUBSCRIPTION_NAME),
    displayDestination = "+1 555-111-2222",
    displaySlotId = 2,
    color = 0xFFE97E6A.toInt(),
)

internal val testVerizonSubscription = Subscription(
    selfParticipantId = "self-1",
    subId = 1,
    label = ConversationSubscriptionLabel.Named(name = TEST_VERIZON_SUBSCRIPTION_NAME),
    displayDestination = "+1 555-867-5309",
    displaySlotId = 1,
    color = 0xFF5E9BE8.toInt(),
)
