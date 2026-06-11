package com.android.messaging.ui.conversation.entry.newchat

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.data.subscription.repository.SubscriptionsRepository
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.participant.IsConversationRecipientLimitExceeded
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversation.entry.NewChatViewModel
import com.android.messaging.ui.conversation.recipientpicker.delegate.ConversationResolutionDelegate
import com.android.messaging.ui.conversation.recipientpicker.delegate.SelectedRecipientsDelegate
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionOutcome
import com.android.messaging.ui.conversation.recipientpicker.model.picker.ConversationResolutionState
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import com.android.messaging.ui.recipientselection.delegate.RecipientPickerDelegate
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseNewChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected fun createViewModel(
        subscriptionsRepository: SubscriptionsRepository =
            createSubscriptionsRepositoryMock(),
        isConversationRecipientLimitExceeded: IsConversationRecipientLimitExceeded =
            createRecipientLimitExceeded(exceeded = false),
        recipientPickerDelegate: RecipientPickerDelegate =
            createRecipientPickerDelegateMock().mock,
        selectedRecipientsDelegate: SelectedRecipientsDelegate =
            createSelectedRecipientsDelegateMock().mock,
        conversationResolutionDelegate: ConversationResolutionDelegate =
            createResolutionDelegateMock().mock,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): NewChatViewModel {
        return NewChatViewModel(
            subscriptionsRepository = subscriptionsRepository,
            isConversationRecipientLimitExceeded = isConversationRecipientLimitExceeded,
            recipientPickerDelegate = recipientPickerDelegate,
            selectedRecipientsDelegate = selectedRecipientsDelegate,
            conversationResolutionDelegate = conversationResolutionDelegate,
            savedStateHandle = savedStateHandle,
            mainDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    protected fun createSubscriptionsRepositoryMock(
        subscriptions: ImmutableList<Subscription> = persistentListOf(),
        defaultSmsSubscriptionId: Int = ParticipantData.DEFAULT_SELF_SUB_ID,
    ): SubscriptionsRepository {
        val mock = mockk<SubscriptionsRepository>()
        every { mock.observeActiveSubscriptions() } returns MutableStateFlow(subscriptions)
        every { mock.getDefaultSmsSubscriptionId() } returns defaultSmsSubscriptionId
        return mock
    }

    protected fun createRecipientLimitExceeded(
        exceeded: Boolean,
    ): IsConversationRecipientLimitExceeded {
        return mockk {
            every { invoke(participantCount = any()) } returns exceeded
        }
    }

    protected fun createRecipientPickerDelegateMock(
        state: RecipientPickerUiState = RecipientPickerUiState(),
    ): RecipientPickerDelegateMock {
        val stateFlow = MutableStateFlow(state)
        val bindScopes = mutableListOf<CoroutineScope>()
        val mock = mockk<RecipientPickerDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.bind(scope = any()) } answers {
            bindScopes += firstArg<CoroutineScope>()
        }
        return RecipientPickerDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            bindScopes = bindScopes,
        )
    }

    protected fun createSelectedRecipientsDelegateMock(
        recipients: ImmutableList<SelectedRecipient> = persistentListOf(),
        toggleOutcome: RecipientToggleOutcome = RecipientToggleOutcome.Added,
    ): SelectedRecipientsDelegateMock {
        val stateFlow = MutableStateFlow(recipients)
        val mock = mockk<SelectedRecipientsDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.toggle(recipient = any(), canAdd = any()) } returns toggleOutcome
        return SelectedRecipientsDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
        )
    }

    protected fun createResolutionDelegateMock(
        state: ConversationResolutionState = ConversationResolutionState.Idle,
    ): ResolutionDelegateMock {
        val stateFlow = MutableStateFlow(state)
        val outcomes = MutableSharedFlow<ConversationResolutionOutcome>()
        val bindScopes = mutableListOf<CoroutineScope>()
        val mock = mockk<ConversationResolutionDelegate>(relaxed = true)
        every { mock.state } returns stateFlow
        every { mock.outcomes } returns outcomes
        every { mock.bind(scope = any()) } answers {
            bindScopes += firstArg<CoroutineScope>()
        }
        return ResolutionDelegateMock(
            mock = mock,
            stateFlow = stateFlow,
            outcomes = outcomes,
            bindScopes = bindScopes,
        )
    }

    protected fun subscription(
        selfParticipantId: String,
        subId: Int = ParticipantData.DEFAULT_SELF_SUB_ID,
        slotId: Int = 1,
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

    protected fun selectedRecipient(
        destination: String,
        label: String = destination,
        displayDestination: String = destination,
    ): SelectedRecipient {
        return SelectedRecipient(
            destination = destination,
            label = label,
            displayDestination = displayDestination,
            photoUri = null,
        )
    }

    protected class RecipientPickerDelegateMock(
        val mock: RecipientPickerDelegate,
        val stateFlow: MutableStateFlow<RecipientPickerUiState>,
        val bindScopes: List<CoroutineScope>,
    )

    protected class SelectedRecipientsDelegateMock(
        val mock: SelectedRecipientsDelegate,
        val stateFlow: MutableStateFlow<ImmutableList<SelectedRecipient>>,
    )

    protected class ResolutionDelegateMock(
        val mock: ConversationResolutionDelegate,
        val stateFlow: MutableStateFlow<ConversationResolutionState>,
        val outcomes: MutableSharedFlow<ConversationResolutionOutcome>,
        val bindScopes: List<CoroutineScope>,
    )

    protected companion object {
        const val DESTINATION = "+15550100"
        const val DESTINATION_2 = "+15550111"
    }
}
