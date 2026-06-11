package com.android.messaging.testutil

import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerListItem
import com.android.messaging.ui.conversationpicker.model.TargetUiState

internal const val TEST_CONTACT_DESTINATION = "+15550001"
internal const val TEST_CONTACT_DISPLAY_NAME = "Alex"
internal const val TEST_CONTACT_SECONDARY_TEXT = "Mobile"
internal const val TEST_RESOLVED_CONVERSATION_ID = "99"

internal fun conversationTarget(
    conversationId: String = "1",
    normalizedDestination: String? = null,
): TargetUiState.Conversation {
    return TargetUiState.Conversation(
        conversationId = conversationId,
        normalizedDestination = normalizedDestination,
        displayName = "Conversation $conversationId",
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
