package com.android.messaging.ui.conversation.recipientpicker.component

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.text.TextRange
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryFieldUiState
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionHiddenBackspaceTargetTest {

    @Test
    fun editableText_usesHiddenBackspaceTargetForEmptyQueryWithSelectedRecipients() {
        val uiState = queryFieldUiState(
            query = "",
            selectedRecipients = persistentListOf(selectedRecipient()),
        )

        val fieldText = recipientSelectionQueryFieldEditableText(uiState = uiState)

        assertEquals("", recipientSelectionVisibleQueryText(fieldText = fieldText))
        assertEquals(1, fieldText.length)
    }

    @Test
    fun editableText_usesQueryWhenQueryIsNotEmpty() {
        val uiState = queryFieldUiState(
            query = "sam",
            selectedRecipients = persistentListOf(selectedRecipient()),
        )

        assertEquals(
            "sam",
            recipientSelectionQueryFieldEditableText(uiState = uiState),
        )
    }

    @Test
    fun hardwareBackspace_removesLastRecipientWhenVisibleQueryIsEmptyAndCursorAtStart() {
        val uiState = queryFieldUiState(
            query = "",
            selectedRecipients = persistentListOf(selectedRecipient()),
        )
        val fieldText = recipientSelectionQueryFieldEditableText(uiState = uiState)

        val shouldRemove = shouldRemoveLastRecipientFromHardwareBackspace(
            keyEvent = backspaceKeyDown(),
            text = fieldText,
            selection = TextRange(index = 1),
            uiState = uiState,
        )

        assertTrue(shouldRemove)
    }

    @Test
    fun hardwareBackspace_doesNotRemoveRecipientWhenFieldIsDisabled() {
        val uiState = queryFieldUiState(
            enabled = false,
            query = "",
            selectedRecipients = persistentListOf(selectedRecipient()),
        )
        val fieldText = recipientSelectionQueryFieldEditableText(uiState = uiState)

        val shouldRemove = shouldRemoveLastRecipientFromHardwareBackspace(
            keyEvent = backspaceKeyDown(),
            text = fieldText,
            selection = TextRange(index = 1),
            uiState = uiState,
        )

        assertFalse(shouldRemove)
    }

    @Test
    fun hiddenBackspaceTargetDeleted_removesLastRecipientOnlyForSentinelDeletion() {
        val uiState = queryFieldUiState(
            query = "",
            selectedRecipients = persistentListOf(selectedRecipient()),
        )
        val fieldText = recipientSelectionQueryFieldEditableText(uiState = uiState)

        assertTrue(
            shouldRemoveLastRecipientAfterHiddenBackspaceTargetDeleted(
                previousText = fieldText,
                nextText = "",
                uiState = uiState,
            ),
        )
        assertFalse(
            shouldRemoveLastRecipientAfterHiddenBackspaceTargetDeleted(
                previousText = fieldText,
                nextText = "a",
                uiState = uiState,
            ),
        )
    }

    private fun queryFieldUiState(
        enabled: Boolean = true,
        query: String,
        selectedRecipients: ImmutableList<SelectedRecipient> = persistentListOf(),
    ): RecipientSelectionQueryFieldUiState {
        return RecipientSelectionQueryFieldUiState(
            query = query,
            enabled = enabled,
            placeholderText = "To",
            selectedRecipients = selectedRecipients,
        )
    }

    private fun selectedRecipient(): SelectedRecipient {
        return SelectedRecipient(
            destination = "+15551234567",
            label = "Sam",
            displayDestination = "(555) 123-4567",
            photoUri = null,
        )
    }

    private fun backspaceKeyDown(): ComposeKeyEvent {
        return ComposeKeyEvent(
            nativeKeyEvent = AndroidKeyEvent(
                AndroidKeyEvent.ACTION_DOWN,
                AndroidKeyEvent.KEYCODE_DEL,
            ),
        )
    }
}
