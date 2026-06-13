package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun RecipientSelectionArmedRecipientResetEffect(
    selectedRecipients: ImmutableList<SelectedRecipient>,
    armedDestination: MutableState<String?>,
) {
    LaunchedEffect(selectedRecipients, armedDestination.value) {
        val current = armedDestination.value ?: return@LaunchedEffect
        val selectedDestinations = selectedRecipients.map { it.destination }

        if (current !in selectedDestinations) {
            armedDestination.value = null
        }
    }
}
