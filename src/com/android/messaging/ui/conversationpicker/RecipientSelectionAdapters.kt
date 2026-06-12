package com.android.messaging.ui.conversationpicker

import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerListItem
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionRowDecorators
import com.android.messaging.ui.conversationpicker.common.pickerContactRowTestTag
import com.android.messaging.ui.conversationpicker.model.ConversationPickerAction as Action
import com.android.messaging.ui.conversationpicker.model.ConversationPickerUiState as State
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import kotlinx.collections.immutable.toImmutableList

internal val pickerContactRowDecorators = RecipientSelectionRowDecorators(
    recipientRowTestTag = { item ->
        pickerContactRowTestTag(item.primaryTestTagKey())
    },
    destinationRowTestTag = { item, destination ->
        pickerContactRowTestTag(item.destinationTestTagKey(destination))
    },
)

internal fun State.asRecipientSelectionState(): RecipientSelectionContentUiState {
    return RecipientSelectionContentUiState(
        picker = contacts,
        selectedRecipients = targets.selection.selectedTargets
            .mapNotNull(TargetUiState::toSelectedRecipient)
            .toImmutableList(),
    )
}

internal fun contactDestinationAction(
    item: RecipientPickerListItem,
    destination: String,
    inSelectionMode: Boolean,
): Action {
    return when {
        inSelectionMode -> Action.ContactDestinationToggled(item, destination)
        else -> Action.ContactDestinationClicked(item, destination)
    }
}

private fun TargetUiState.toSelectedRecipient(): SelectedRecipient? {
    val destination = normalizedDestination ?: return null

    return SelectedRecipient(
        destination = destination,
        label = displayName,
        displayDestination = details.orEmpty(),
        photoUri = avatarUri,
    )
}

private fun RecipientPickerListItem.primaryTestTagKey(): String {
    return when (this) {
        is RecipientPickerListItem.Contact -> {
            val singleDestination = destinations.singleOrNull()

            when {
                singleDestination != null -> "$id:${singleDestination.dataId}"
                else -> id
            }
        }

        is RecipientPickerListItem.SyntheticPhone -> id
    }
}

private fun RecipientPickerListItem.destinationTestTagKey(destination: String): String {
    return when (this) {
        is RecipientPickerListItem.Contact -> {
            val matchingDestination = destinations.firstOrNull { contactDestination ->
                contactDestination.value == destination ||
                    contactDestination.normalizedValue == destination
            }

            when {
                matchingDestination != null -> "$id:${matchingDestination.dataId}"
                else -> "$id:$destination"
            }
        }

        is RecipientPickerListItem.SyntheticPhone -> id
    }
}
