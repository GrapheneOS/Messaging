package com.android.messaging.testutil

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem

internal const val TEST_CONTACT_DESTINATION = "+15550001"
internal const val TEST_CONTACT_DISPLAY_NAME = "Alex"
internal const val TEST_CONTACT_SECONDARY_TEXT = "Mobile"
internal val TEST_RESOLVED_CONVERSATION_ID = ConversationId("99")

internal fun conversationTarget(
    conversationId: ConversationId = ConversationId("1"),
    normalizedDestination: String? = null,
): TargetUiState.Conversation {
    return TargetUiState.Conversation(
        conversationId = conversationId,
        normalizedDestination = normalizedDestination,
        displayName = "Conversation ${conversationId.value}",
        details = null,
        avatarUri = null,
        isGroup = false,
    )
}

internal fun contactTarget(
    contactId: Long = 1L,
    destination: String = TEST_CONTACT_DESTINATION,
): TargetUiState.Contact {
    return TargetUiState.Contact(
        destination = destination,
        normalizedDestination = destination,
        displayName = "Contact $contactId",
        details = null,
        avatarUri = null,
    )
}

internal fun syntheticPhoneItem(
    normalizedDestination: String = TEST_CONTACT_DESTINATION,
    displayName: String = TEST_CONTACT_DISPLAY_NAME,
    secondaryText: String = TEST_CONTACT_SECONDARY_TEXT,
): RecipientPickerListItem.SyntheticPhone {
    return RecipientPickerListItem.SyntheticPhone(
        id = "synthetic:$normalizedDestination",
        rawQuery = normalizedDestination,
        destination = normalizedDestination,
        normalizedDestination = normalizedDestination,
        displayName = displayName,
        secondaryText = secondaryText,
    )
}
