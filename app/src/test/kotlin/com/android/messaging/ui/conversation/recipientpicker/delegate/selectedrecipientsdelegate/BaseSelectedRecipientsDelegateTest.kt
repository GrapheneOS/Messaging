package com.android.messaging.ui.conversation.recipientpicker.delegate.selectedrecipientsdelegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.ui.conversation.recipientpicker.delegate.SelectedRecipientsDelegateImpl
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import io.mockk.spyk

internal abstract class BaseSelectedRecipientsDelegateTest {

    protected lateinit var savedStateHandle: SavedStateHandle

    protected fun createDelegate(
        initialRecipients: List<SelectedRecipient> = emptyList(),
    ): SelectedRecipientsDelegateImpl {
        savedStateHandle = spyk(
            SavedStateHandle(
                initialState = when {
                    initialRecipients.isEmpty() -> emptyMap()
                    else -> mapOf(SELECTED_RECIPIENTS_KEY to ArrayList(initialRecipients))
                },
            ),
        )
        return SelectedRecipientsDelegateImpl(savedStateHandle = savedStateHandle)
    }

    protected fun recipient(
        destination: String,
        label: String = "Label $destination",
        displayDestination: String = "Display $destination",
        photoUri: String? = null,
    ): SelectedRecipient {
        return SelectedRecipient(
            destination = destination,
            label = label,
            displayDestination = displayDestination,
            photoUri = photoUri,
        )
    }

    protected fun persistedRecipients(): List<SelectedRecipient>? {
        return savedStateHandle
            .get<ArrayList<SelectedRecipient>>(SELECTED_RECIPIENTS_KEY)
            ?.toList()
    }

    protected companion object {
        const val SELECTED_RECIPIENTS_KEY = "selected_recipients"
    }
}
