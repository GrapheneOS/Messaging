package com.android.messaging.data.subscription

import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class SubscriptionSelectionResolverTest {

    private val firstSubscription = createSubscription(
        selfParticipantId = FIRST_SELF_PARTICIPANT_ID,
        subId = FIRST_SUB_ID,
        slotId = 1,
    )
    private val secondSubscription = createSubscription(
        selfParticipantId = SECOND_SELF_PARTICIPANT_ID,
        subId = SECOND_SUB_ID,
        slotId = 2,
    )
    private val subscriptions = persistentListOf(firstSubscription, secondSubscription)

    @Test
    fun resolve_selfParticipantIdMatches_selectsThatSubscription() {
        val selectedSubscription = resolveSelectedSubscription(
            subscriptions = subscriptions,
            selectedSelfParticipantId = ParticipantId(SECOND_SELF_PARTICIPANT_ID),
            defaultSmsSubscriptionId = SubId(FIRST_SUB_ID),
        )

        assertEquals(secondSubscription, selectedSubscription)
    }

    @Test
    fun resolve_selfParticipantIdMatchesNothing_selectsDefaultSmsSubscription() {
        val selectedSubscription = resolveSelectedSubscription(
            subscriptions = subscriptions,
            selectedSelfParticipantId = ParticipantId(UNKNOWN_SELF_PARTICIPANT_ID),
            defaultSmsSubscriptionId = SubId(SECOND_SUB_ID),
        )

        assertEquals(secondSubscription, selectedSubscription)
    }

    @Test
    fun resolve_selfParticipantIdIsNull_selectsDefaultSmsSubscription() {
        val selectedSubscription = resolveSelectedSubscription(
            subscriptions = subscriptions,
            selectedSelfParticipantId = null,
            defaultSmsSubscriptionId = SubId(SECOND_SUB_ID),
        )

        assertEquals(secondSubscription, selectedSubscription)
    }

    @Test
    fun resolve_defaultSmsSubscriptionIsNotActive_selectsFirstSubscription() {
        val selectedSubscription = resolveSelectedSubscription(
            subscriptions = subscriptions,
            selectedSelfParticipantId = null,
            defaultSmsSubscriptionId = SubId(REMOVED_SUB_ID),
        )

        assertEquals(firstSubscription, selectedSubscription)
    }

    @Test
    fun resolve_defaultSmsSubscriptionIdIsUnset_ignoresEmulatedSubscription() {
        val emulatedSubscription = createSubscription(
            selfParticipantId = EMULATED_SELF_PARTICIPANT_ID,
            subId = ParticipantData.DEFAULT_SELF_SUB_ID,
            slotId = 2,
        )

        val selectedSubscription = resolveSelectedSubscription(
            subscriptions = persistentListOf(firstSubscription, emulatedSubscription),
            selectedSelfParticipantId = null,
            defaultSmsSubscriptionId = SubId(ParticipantData.DEFAULT_SELF_SUB_ID),
        )

        assertEquals(firstSubscription, selectedSubscription)
    }

    @Test
    fun resolve_noSubscriptions_returnsNull() {
        val selectedSubscription = resolveSelectedSubscription(
            subscriptions = persistentListOf(),
            selectedSelfParticipantId = ParticipantId(FIRST_SELF_PARTICIPANT_ID),
            defaultSmsSubscriptionId = SubId(FIRST_SUB_ID),
        )

        assertNull(selectedSubscription)
    }

    private fun createSubscription(
        selfParticipantId: String,
        subId: Int,
        slotId: Int,
    ): Subscription {
        return Subscription(
            selfParticipantId = ParticipantId(selfParticipantId),
            subId = SubId(subId),
            label = ConversationSubscriptionLabel.Slot(slotId = slotId),
            displayDestination = null,
            displaySlotId = slotId,
            color = 0,
        )
    }

    private companion object {
        private const val FIRST_SELF_PARTICIPANT_ID = "self-participant-1"
        private const val SECOND_SELF_PARTICIPANT_ID = "self-participant-2"
        private const val UNKNOWN_SELF_PARTICIPANT_ID = "self-participant-default"
        private const val EMULATED_SELF_PARTICIPANT_ID = "debug_sim_emulated_2"
        private const val FIRST_SUB_ID = 11
        private const val SECOND_SUB_ID = 12
        private const val REMOVED_SUB_ID = 13
    }
}
