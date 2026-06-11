package com.android.messaging.ui.conversationsettings.screen.support

import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal const val ROOT_CONVERSATION_ID = "conversation-1"
internal const val PARTICIPANT_CONVERSATION_ID = "conversation-2"

internal const val FINISH_RESULT_CODE = 1

internal const val MOTHER_NAME = "Mother"
internal const val FATHER_NAME = "Father"

internal const val MOTHER_PARTICIPANT_ID = "mother"
internal const val FATHER_PARTICIPANT_ID = "father"
internal const val TEST_PARTICIPANT_ID = "test_participant"

internal const val ONE_TO_ONE_TITLE = MOTHER_NAME
internal const val GROUP_TITLE = "Family"

internal const val TEST_DESTINATION = "+31612345678"
internal const val MOTHER_DESTINATION = "+31611111111"
internal const val FATHER_DESTINATION = "+31622222222"

internal const val SUB1_ID = "sub1"
internal const val SUB2_ID = "sub2"
internal const val SUB1_DESTINATION = "+11111111111"
internal const val SUB2_DESTINATION = "+22222222222"

internal const val SUB1_SLOT = 1
internal const val SUB2_SLOT = 2

internal fun oneToOneState(
    isSnoozed: Boolean = false,
    isArchived: Boolean = false,
    canCall: Boolean = true,
    canShowContact: Boolean = true,
    isContactSaved: Boolean = false,
    isSimSwitchAvailable: Boolean = false,
    availableSubscriptions: ImmutableList<Subscription> = persistentListOf(),
    selectedSubscription: Subscription? = null,
    otherParticipant: ParticipantUiState = participant(),
): ConversationSettingsUiState {
    return ConversationSettingsUiState(
        conversationId = ROOT_CONVERSATION_ID,
        conversationTitle = ONE_TO_ONE_TITLE,
        isArchived = isArchived,
        isSnoozed = isSnoozed,
        participants = persistentListOf(otherParticipant),
        otherParticipant = otherParticipant,
        availableSubscriptions = availableSubscriptions,
        selectedSubscription = selectedSubscription,
        isSimSwitchAvailable = isSimSwitchAvailable,
        canCall = canCall,
        canShowContact = canShowContact,
        isContactSaved = isContactSaved,
    )
}

internal fun groupState(): ConversationSettingsUiState {
    return ConversationSettingsUiState(
        conversationId = ROOT_CONVERSATION_ID,
        conversationTitle = GROUP_TITLE,
        participants = persistentListOf(
            participant(
                id = MOTHER_PARTICIPANT_ID,
                displayName = MOTHER_NAME,
                displayDestination = MOTHER_DESTINATION,
            ),
            participant(
                id = FATHER_PARTICIPANT_ID,
                displayName = FATHER_NAME,
                displayDestination = FATHER_DESTINATION,
            ),
        ),
        otherParticipant = null,
        canCall = false,
        canShowContact = false,
    )
}

internal fun participant(
    id: String = TEST_PARTICIPANT_ID,
    displayName: String = MOTHER_NAME,
    displayDestination: String = TEST_DESTINATION,
    isBlocked: Boolean = false,
    canCall: Boolean = true,
    isContactSaved: Boolean = true,
): ParticipantUiState {
    return ParticipantUiState(
        id = id,
        avatarUri = null,
        displayName = displayName,
        details = displayDestination,
        contactId = 1L,
        lookupKey = null,
        normalizedDestination = displayDestination,
        isBlocked = isBlocked,
        displayDestination = displayDestination,
        canCall = canCall,
        isContactSaved = isContactSaved,
    )
}

internal fun subscription(
    id: String,
    slotId: Int,
    destination: String?,
): Subscription {
    return Subscription(
        selfParticipantId = id,
        subId = slotId,
        label = ConversationSubscriptionLabel.Slot(slotId = slotId),
        displayDestination = destination,
        displaySlotId = slotId,
        color = 0,
    )
}
