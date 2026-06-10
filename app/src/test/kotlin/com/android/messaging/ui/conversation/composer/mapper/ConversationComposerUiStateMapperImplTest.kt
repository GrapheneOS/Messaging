package com.android.messaging.ui.conversation.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationComposerUiStateMapperImplTest {

    private val mapper = ConversationComposerUiStateMapperImpl()

    @Test
    fun map_withoutDraftSelfParticipant_usesDefaultSmsSubscription() {
        val firstSubscription = firstSubscription()
        val secondSubscription = secondSubscription()

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(firstSubscription, secondSubscription),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = SECOND_SUB_ID,
        )

        assertEquals(secondSubscription, uiState.simSelector.selectedSubscription)
    }

    @Test
    fun map_withDraftSelfParticipant_keepsExplicitSelectionBeforeDefaultSmsSubscription() {
        val firstSubscription = firstSubscription()
        val secondSubscription = secondSubscription()

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    selfParticipantId = FIRST_SELF_PARTICIPANT_ID,
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(firstSubscription, secondSubscription),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = SECOND_SUB_ID,
        )

        assertEquals(firstSubscription, uiState.simSelector.selectedSubscription)
    }

    @Test
    fun map_withoutResolvedDefaultSmsSubscription_fallsBackToFirstSubscription() {
        val firstSubscription = firstSubscription()
        val secondSubscription = secondSubscription()

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(firstSubscription, secondSubscription),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertEquals(firstSubscription, uiState.simSelector.selectedSubscription)
    }

    @Test
    fun map_withStaleDefaultSmsSubscription_fallsBackToFirstSubscription() {
        val firstSubscription = firstSubscription()
        val secondSubscription = secondSubscription()

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(firstSubscription, secondSubscription),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = STALE_SUB_ID,
        )

        assertEquals(firstSubscription, uiState.simSelector.selectedSubscription)
    }

    @Test
    fun map_withStaleDraftSelfParticipant_fallsBackToDefaultSmsSubscription() {
        val firstSubscription = firstSubscription()
        val secondSubscription = secondSubscription()

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    selfParticipantId = STALE_SELF_PARTICIPANT_ID,
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(firstSubscription, secondSubscription),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = SECOND_SUB_ID,
        )

        assertEquals(secondSubscription, uiState.simSelector.selectedSubscription)
    }

    @Test
    fun map_withoutSubscriptions_selectsNoSubscription() {
        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = false,
            defaultSmsSubscriptionId = SECOND_SUB_ID,
        )

        assertNull(uiState.simSelector.selectedSubscription)
        assertTrue(uiState.simSelector.isLoading)
    }

    private fun firstSubscription(): Subscription {
        return Subscription(
            selfParticipantId = FIRST_SELF_PARTICIPANT_ID,
            subId = FIRST_SUB_ID,
            label = ConversationSubscriptionLabel.Named(name = "SIM 1"),
            displayDestination = null,
            displaySlotId = 1,
            color = 0,
        )
    }

    private fun secondSubscription(): Subscription {
        return Subscription(
            selfParticipantId = SECOND_SELF_PARTICIPANT_ID,
            subId = SECOND_SUB_ID,
            label = ConversationSubscriptionLabel.Named(name = "SIM 2"),
            displayDestination = null,
            displaySlotId = 2,
            color = 0,
        )
    }

    private companion object {
        private const val FIRST_SELF_PARTICIPANT_ID = "self-participant-1"
        private const val SECOND_SELF_PARTICIPANT_ID = "self-participant-2"
        private const val STALE_SELF_PARTICIPANT_ID = "self-participant-gone"
        private const val FIRST_SUB_ID = 1
        private const val SECOND_SUB_ID = 2
        private const val STALE_SUB_ID = 99
    }
}
