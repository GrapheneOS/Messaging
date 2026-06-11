package com.android.messaging.ui.conversation.composer.mapper

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationComposerDisabledReason
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.MessageTextStats
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.sms.MmsConfig
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.conversation.composer.model.ConversationSegmentCounterUiState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationComposerUiStateMapperImplTest {

    private val mapper = ConversationComposerUiStateMapperImpl()

    @Before
    fun setUp() {
        val mmsConfig = mockk<MmsConfig>()
        every { mmsConfig.multipartSmsEnabled } returns true
        every { mmsConfig.sendMultipartSmsAsSeparateMessages } returns false
        every { mmsConfig.smsToMmsTextLengthThreshold } returns -1
        every { mmsConfig.smsToMmsTextThreshold } returns -1
        mockkStatic(MmsConfig::class)
        every { MmsConfig.get(any()) } returns mmsConfig

        mockkConstructor(MessageTextStats::class)
        every {
            anyConstructed<MessageTextStats>().updateMessageTextStats(any(), any())
        } just runs
        every { anyConstructed<MessageTextStats>().numMessagesToBeSent } returns 1
        every {
            anyConstructed<MessageTextStats>().codePointsRemainingInCurrentMessage
        } returns Int.MAX_VALUE
    }

    @After
    fun tearDown() {
        unmockkConstructor(MessageTextStats::class)
        unmockkStatic(MmsConfig::class)
    }

    @Test
    fun map_enablesSendOnlyWhenContentIsAvailableAndDraftIsIdle() {
        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertTrue(uiState.isSendEnabled)
    }

    @Test
    fun map_disablesSendWhenDraftIsEmpty() {
        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertFalse(uiState.isSendEnabled)
    }

    @Test
    fun map_disablesSendAndAttachmentWhenDraftIsBusy() {
        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                    isCheckingDraft = true,
                    isSending = true,
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertFalse(uiState.isAttachmentActionEnabled)
        assertFalse(uiState.isSendEnabled)
    }

    @Test
    fun map_keepsMessageFieldTiedToAvailabilityOnly() {
        val unavailableAvailability = ConversationComposerAvailability.Unavailable(
            reason = ConversationComposerDisabledReason.CONVERSATION_UNAVAILABLE,
        )

        val unavailableUiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                    isCheckingDraft = true,
                    isSending = true,
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = unavailableAvailability,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        val availableUiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                    isCheckingDraft = true,
                    isSending = true,
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertFalse(unavailableUiState.isMessageFieldEnabled)
        assertTrue(availableUiState.isMessageFieldEnabled)
    }

    @Test
    fun map_preservesSendProtocolForWorkingDraft() {
        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                ),
                sendProtocol = ConversationDraftSendProtocol.MMS,
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertEquals(ConversationDraftSendProtocol.MMS, uiState.sendProtocol)
    }

    @Test
    fun map_resetsSendProtocolToSmsForEmptyDraft() {
        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(),
                sendProtocol = ConversationDraftSendProtocol.MMS,
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertEquals(ConversationDraftSendProtocol.SMS, uiState.sendProtocol)
    }

    @Test
    fun map_hidesSegmentCounterWhenSmsMessageIsNotNearBoundary() {
        every { anyConstructed<MessageTextStats>().numMessagesToBeSent } returns 1
        every {
            anyConstructed<MessageTextStats>().codePointsRemainingInCurrentMessage
        } returns 11

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Hello",
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertNull(uiState.segmentCounter)
    }

    @Test
    fun map_showsSingleSegmentCounterWhenSmsMessageIsNearBoundary() {
        val messageText = "Almost full"
        every { anyConstructed<MessageTextStats>().numMessagesToBeSent } returns 1
        every {
            anyConstructed<MessageTextStats>().codePointsRemainingInCurrentMessage
        } returns 10

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = messageText,
                    selfParticipantId = "sub-b",
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(
                createSubscription(
                    selfParticipantId = "sub-a",
                    subId = FIRST_SUB_ID,
                    slotId = 1,
                ),
                createSubscription(
                    selfParticipantId = "sub-b",
                    subId = SECOND_SUB_ID,
                    slotId = 2,
                ),
            ),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = FIRST_SUB_ID,
        )

        assertEquals(
            ConversationSegmentCounterUiState(
                codePointsRemainingInCurrentMessage = 10,
                messageCount = 1,
            ),
            uiState.segmentCounter,
        )
        verify(exactly = 1) {
            anyConstructed<MessageTextStats>().updateMessageTextStats(
                SECOND_SUB_ID,
                messageText,
            )
        }
    }

    @Test
    fun map_showsMultiSegmentCounterWhenSmsMessageSpansMultipleMessages() {
        every { anyConstructed<MessageTextStats>().numMessagesToBeSent } returns 3
        every {
            anyConstructed<MessageTextStats>().codePointsRemainingInCurrentMessage
        } returns 82

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Long message",
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertEquals(
            ConversationSegmentCounterUiState(
                codePointsRemainingInCurrentMessage = 82,
                messageCount = 3,
            ),
            uiState.segmentCounter,
        )
    }

    @Test
    fun map_hidesSegmentCounterForMmsDraft() {
        every { anyConstructed<MessageTextStats>().numMessagesToBeSent } returns 3
        every {
            anyConstructed<MessageTextStats>().codePointsRemainingInCurrentMessage
        } returns 10

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Long message",
                ),
                sendProtocol = ConversationDraftSendProtocol.MMS,
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertNull(uiState.segmentCounter)
    }

    @Test
    fun map_usesDefaultSelfSubIdForSegmentCounterWhenNoSubscriptionIsSelected() {
        every { anyConstructed<MessageTextStats>().numMessagesToBeSent } returns 1
        every {
            anyConstructed<MessageTextStats>().codePointsRemainingInCurrentMessage
        } returns 10

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Almost full",
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertEquals(
            ConversationSegmentCounterUiState(
                codePointsRemainingInCurrentMessage = 10,
                messageCount = 1,
            ),
            uiState.segmentCounter,
        )
        verify(exactly = 1) {
            anyConstructed<MessageTextStats>().updateMessageTextStats(
                ParticipantData.DEFAULT_SELF_SUB_ID,
                "Almost full",
            )
        }
    }

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

    @Test
    fun map_selectsSubscriptionMatchingDraftSelfParticipantId() {
        val matchingSubscription = createSubscription(
            selfParticipantId = "sub-b",
            subId = SECOND_SUB_ID,
            slotId = 2,
        )
        val subscriptions = persistentListOf(
            createSubscription(
                selfParticipantId = "sub-a",
                subId = FIRST_SUB_ID,
                slotId = 1,
            ),
            matchingSubscription,
        )

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    selfParticipantId = "sub-b",
                ),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = subscriptions,
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = FIRST_SUB_ID,
        )

        assertEquals(matchingSubscription, uiState.simSelector.selectedSubscription)
        assertEquals(subscriptions, uiState.simSelector.subscriptions)
        assertTrue(uiState.simSelector.isAvailable)
    }

    @Test
    fun map_leavesSimSelectorUnavailableForSingleOrEmptySubscriptionList() {
        val emptyUiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        val singleUiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(),
            ),
            attachments = persistentListOf(),
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(
                createSubscription(
                    selfParticipantId = "sub-a",
                    subId = FIRST_SUB_ID,
                    slotId = 1,
                ),
            ),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = FIRST_SUB_ID,
        )

        assertFalse(emptyUiState.simSelector.isAvailable)
        assertNull(emptyUiState.simSelector.selectedSubscription)
        assertFalse(singleUiState.simSelector.isAvailable)
    }

    @Test
    fun map_preservesProvidedAttachments() {
        val attachments = persistentListOf<ComposerAttachmentUiModel>(
            ComposerAttachmentUiModel.Resolved.File(
                key = "attachment-1",
                contentType = "application/pdf",
                contentUri = "content://provided/attachment/1",
            ),
        )

        val uiState = mapper.map(
            audioRecording = ConversationAudioRecordingUiState(),
            draftState = ConversationDraftState(
                draft = ConversationDraft(
                    messageText = "Draft text",
                ),
            ),
            attachments = attachments,
            composerAvailability = ConversationComposerAvailability.Editable,
            subscriptions = persistentListOf(),
            areSubscriptionsLoaded = true,
            defaultSmsSubscriptionId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        assertEquals(attachments, uiState.attachments)
    }

    private fun firstSubscription(): Subscription {
        return createSubscription(
            selfParticipantId = FIRST_SELF_PARTICIPANT_ID,
            subId = FIRST_SUB_ID,
            slotId = 1,
        )
    }

    private fun secondSubscription(): Subscription {
        return createSubscription(
            selfParticipantId = SECOND_SELF_PARTICIPANT_ID,
            subId = SECOND_SUB_ID,
            slotId = 2,
        )
    }

    private fun createSubscription(
        selfParticipantId: String,
        subId: Int,
        slotId: Int,
    ): Subscription {
        return Subscription(
            selfParticipantId = selfParticipantId,
            subId = subId,
            label = ConversationSubscriptionLabel.Slot(slotId = slotId),
            displayDestination = null,
            displaySlotId = slotId,
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
