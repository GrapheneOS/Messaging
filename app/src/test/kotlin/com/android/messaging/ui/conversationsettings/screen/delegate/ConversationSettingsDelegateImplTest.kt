package com.android.messaging.ui.conversationsettings.screen.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.blockedparticipants.repository.BlockedParticipantsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepository
import com.android.messaging.data.conversationsettings.repository.ConversationSettingsRepository
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.domain.conversationsettings.usecase.SetConversationSelfParticipantId
import com.android.messaging.ui.conversationsettings.screen.CONVERSATION_SETTINGS_CONVERSATION_ID_ARG
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapperImpl
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationSettingsDelegateImplTest {

    private val settingsRepository = mockk<ConversationSettingsRepository>()
    private val subscriptionsRepository = mockk<SubscriptionsRepository>()
    private val setConversationSelfParticipantId =
        mockk<SetConversationSelfParticipantId>(relaxed = true)

    @Test
    fun bind_conversationBoundToDefaultSelf_showsDefaultSmsSubscription() {
        runTest {
            stubConversationSelfParticipantId(ParticipantId(DEFAULT_SELF_PARTICIPANT_ID))
            val delegate = createDelegate(applicationScope = backgroundScope)

            delegate.bind(backgroundScope)
            runCurrent()

            assertEquals(SECOND_SUBSCRIPTION, delegate.state.value.selectedSubscription)
        }
    }

    @Test
    fun bind_conversationBoundToSubscription_showsThatSubscription() {
        runTest {
            stubConversationSelfParticipantId(ParticipantId(FIRST_SELF_PARTICIPANT_ID))
            val delegate = createDelegate(applicationScope = backgroundScope)

            delegate.bind(backgroundScope)
            runCurrent()

            assertEquals(FIRST_SUBSCRIPTION, delegate.state.value.selectedSubscription)
        }
    }

    @Test
    fun setSelfParticipantId_tappingTheImplicitlySelectedSubscription_pinsIt() {
        runTest {
            stubConversationSelfParticipantId(ParticipantId(DEFAULT_SELF_PARTICIPANT_ID))
            val delegate = createDelegate(applicationScope = backgroundScope)

            delegate.bind(backgroundScope)
            runCurrent()
            delegate.setSelfParticipantId(ParticipantId(SECOND_SELF_PARTICIPANT_ID))
            runCurrent()

            coVerify(exactly = 1) {
                setConversationSelfParticipantId(
                    conversationId = CONVERSATION_ID,
                    selfParticipantId = ParticipantId(SECOND_SELF_PARTICIPANT_ID),
                )
            }
        }
    }

    @Test
    fun setSelfParticipantId_tappingTheAlreadySelectedSubscription_isIgnored() {
        runTest {
            stubConversationSelfParticipantId(ParticipantId(FIRST_SELF_PARTICIPANT_ID))
            val delegate = createDelegate(applicationScope = backgroundScope)

            delegate.bind(backgroundScope)
            runCurrent()
            delegate.setSelfParticipantId(ParticipantId(FIRST_SELF_PARTICIPANT_ID))
            runCurrent()

            coVerify(exactly = 0) { setConversationSelfParticipantId(any(), any()) }
        }
    }

    private fun stubConversationSelfParticipantId(selfParticipantId: ParticipantId) {
        every { settingsRepository.getConversationSettings(CONVERSATION_ID) } returns flowOf(
            ConversationSettingsData(
                conversationId = CONVERSATION_ID,
                dbSelfParticipantId = selfParticipantId,
            ),
        )
        every { subscriptionsRepository.observeActiveSubscriptions() } returns flowOf(
            persistentListOf(FIRST_SUBSCRIPTION, SECOND_SUBSCRIPTION),
        )
        every { subscriptionsRepository.observeDefaultSmsSubscriptionId() } returns flowOf(
            SubId(SECOND_SUB_ID),
        )
    }

    private fun createDelegate(applicationScope: CoroutineScope): ConversationSettingsDelegateImpl {
        return ConversationSettingsDelegateImpl(
            repository = settingsRepository,
            notificationRepository = mockk<ConversationNotificationRepository>(relaxed = true),
            subscriptionsRepository = subscriptionsRepository,
            mapper = ConversationSettingsUiStateMapperImpl(
                canPlacePhoneCall = { false },
                canShowOrAddContact = { _, _, _, _ -> false },
                isContactSavedUseCase = { _, _ -> false },
            ),
            conversationsRepository = mockk<ConversationsRepository>(relaxed = true),
            blockedParticipantsRepository = mockk<BlockedParticipantsRepository>(relaxed = true),
            setConversationSelfParticipantId = setConversationSelfParticipantId,
            applicationScope = applicationScope,
            savedStateHandle = SavedStateHandle(
                mapOf(CONVERSATION_SETTINGS_CONVERSATION_ID_ARG to CONVERSATION_ID.value),
            ),
        )
    }

    private companion object {
        private val CONVERSATION_ID = ConversationId("conversation-1")
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
