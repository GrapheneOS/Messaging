package com.android.messaging.ui.conversationsettings.screen.mapper

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class ConversationSettingsUiStateMapperImplTest {

    private val mapper = ConversationSettingsUiStateMapperImpl(
        canPlacePhoneCall = { false },
        canShowOrAddContact = { _, _, _, _ -> false },
        isContactSavedUseCase = { _, _ -> false },
    )

    @Test
    fun map_unsavedNumberWithNullFullName_usesFormattedDestinationAsDisplayName() {
        val participantUiState = mapParticipant(
            name = null,
            unknownSender = true,
        )

        assertEquals(DISPLAY_DESTINATION, participantUiState.displayName)
        assertNull(participantUiState.details)
    }

    @Test
    fun map_savedContact_usesFullNameAndKeepsFormattedDestinationAsDetails() {
        val participantUiState = mapParticipant(
            name = FULL_NAME,
            unknownSender = false,
        )

        assertEquals(FULL_NAME, participantUiState.displayName)
        assertEquals(DISPLAY_DESTINATION, participantUiState.details)
    }

    @Test
    fun map_conversationBoundToDefaultSelf_selectsDefaultSmsSubscription() {
        val uiState = mapper.map(
            data = ConversationSettingsData(
                conversationId = CONVERSATION_ID,
                dbSelfParticipantId = ParticipantId(DEFAULT_SELF_PARTICIPANT_ID),
            ),
            subscriptions = persistentListOf(FIRST_SUBSCRIPTION, SECOND_SUBSCRIPTION),
            defaultSmsSubscriptionId = SubId(SECOND_SUB_ID),
        )

        assertEquals(SECOND_SUBSCRIPTION, uiState.selectedSubscription)
    }

    @Test
    fun map_conversationBoundToSubscription_selectsThatSubscription() {
        val uiState = mapper.map(
            data = ConversationSettingsData(
                conversationId = CONVERSATION_ID,
                dbSelfParticipantId = ParticipantId(FIRST_SELF_PARTICIPANT_ID),
            ),
            subscriptions = persistentListOf(FIRST_SUBSCRIPTION, SECOND_SUBSCRIPTION),
            defaultSmsSubscriptionId = SubId(SECOND_SUB_ID),
        )

        assertEquals(FIRST_SUBSCRIPTION, uiState.selectedSubscription)
    }

    @Test
    fun map_conversationBoundToDefaultSelf_keepsUnresolvedSelfParticipantId() {
        val uiState = mapper.map(
            data = ConversationSettingsData(
                conversationId = CONVERSATION_ID,
                dbSelfParticipantId = ParticipantId(DEFAULT_SELF_PARTICIPANT_ID),
            ),
            subscriptions = persistentListOf(FIRST_SUBSCRIPTION, SECOND_SUBSCRIPTION),
            defaultSmsSubscriptionId = SubId(SECOND_SUB_ID),
        )

        assertEquals(ParticipantId(DEFAULT_SELF_PARTICIPANT_ID), uiState.selfParticipantId)
    }

    private fun mapParticipant(
        name: String?,
        unknownSender: Boolean,
    ): ParticipantUiState {
        val participant = mockk<ParticipantData>(relaxed = true) {
            every { fullName } returns name
            every { sendDestination } returns SEND_DESTINATION
            every { displayDestination } returns DISPLAY_DESTINATION
            every { isUnknownSender } returns unknownSender
        }

        return mapper
            .map(
                data = ConversationSettingsData(
                    conversationId = CONVERSATION_ID,
                    participants = persistentListOf(participant),
                ),
                subscriptions = persistentListOf(),
                defaultSmsSubscriptionId = SubId(ParticipantData.DEFAULT_SELF_SUB_ID),
            )
            .participants
            .single()
    }

    private companion object {
        private val CONVERSATION_ID = ConversationId("conversation-1")
        private const val SEND_DESTINATION = "+15550123"
        private const val DISPLAY_DESTINATION = "+1 555-0123"
        private const val FULL_NAME = "Ada Lovelace"
        private const val DEFAULT_SELF_PARTICIPANT_ID = "self-participant-default"
        private const val FIRST_SELF_PARTICIPANT_ID = "self-participant-1"
        private const val SECOND_SELF_PARTICIPANT_ID = "self-participant-2"
        private const val FIRST_SUB_ID = 1
        private const val SECOND_SUB_ID = 2

        private val FIRST_SUBSCRIPTION = Subscription(
            selfParticipantId = ParticipantId(FIRST_SELF_PARTICIPANT_ID),
            subId = SubId(FIRST_SUB_ID),
            label = ConversationSubscriptionLabel.Slot(slotId = 1),
            displayDestination = null,
            displaySlotId = 1,
            color = 0,
        )
        private val SECOND_SUBSCRIPTION = Subscription(
            selfParticipantId = ParticipantId(SECOND_SELF_PARTICIPANT_ID),
            subId = SubId(SECOND_SUB_ID),
            label = ConversationSubscriptionLabel.Slot(slotId = 2),
            displayDestination = null,
            displaySlotId = 2,
            color = 0,
        )
    }
}
