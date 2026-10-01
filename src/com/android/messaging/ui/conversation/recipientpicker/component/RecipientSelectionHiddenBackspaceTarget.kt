package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryFieldUiState

// Some soft keyboards do not emit key events for Backspace on a visually empty field
private const val BACKSPACE_SENTINEL_CHAR = '⁠'
private const val BACKSPACE_SENTINEL = BACKSPACE_SENTINEL_CHAR.toString()

internal object RecipientSelectionHiddenBackspaceTargetOutputTransformation : OutputTransformation {

    override fun TextFieldBuffer.transformOutput() {
        if (length > 0 && asCharSequence()[0] == BACKSPACE_SENTINEL_CHAR) {
            delete(0, 1)
        }
    }
}

internal object RecipientSelectionHiddenBackspaceTargetInputTransformation : InputTransformation {

    override fun TextFieldBuffer.transformInput() {
        // A tap can put the cursor before the sentinel, so text can land on either side of it
        if (length > 1) {
            for (index in length - 1 downTo 0) {
                if (charAt(index) == BACKSPACE_SENTINEL_CHAR) {
                    delete(index, index + 1)
                }
            }
        }
    }
}

internal fun recipientSelectionQueryFieldEditableText(
    uiState: RecipientSelectionQueryFieldUiState,
): String {
    return when {
        shouldUseHiddenBackspaceTarget(uiState = uiState) -> BACKSPACE_SENTINEL
        else -> uiState.query
    }
}

internal fun recipientSelectionVisibleQueryText(fieldText: String): String {
    return fieldText.removePrefix(BACKSPACE_SENTINEL)
}

private fun shouldUseHiddenBackspaceTarget(
    uiState: RecipientSelectionQueryFieldUiState,
): Boolean {
    return uiState.query.isEmpty() && uiState.selectedRecipients.isNotEmpty()
}

internal fun shouldRemoveLastRecipientFromHardwareBackspace(
    keyEvent: KeyEvent,
    text: CharSequence,
    selection: TextRange,
    uiState: RecipientSelectionQueryFieldUiState,
): Boolean {
    return keyEvent.key == Key.Backspace &&
        keyEvent.type == KeyEventType.KeyDown &&
        shouldRemoveLastRecipientFromVisibleEmptyQueryBackspace(
            text = text,
            selection = selection,
            uiState = uiState,
        )
}

private fun shouldRemoveLastRecipientFromVisibleEmptyQueryBackspace(
    text: CharSequence,
    selection: TextRange,
    uiState: RecipientSelectionQueryFieldUiState,
): Boolean {
    val sentinelLength = BACKSPACE_SENTINEL.length
    val visibleText = recipientSelectionVisibleQueryText(fieldText = text.toString())
    val visibleSelectionStart = (selection.start - sentinelLength).coerceAtLeast(minimumValue = 0)
    val visibleSelectionEnd = (selection.end - sentinelLength).coerceAtLeast(minimumValue = 0)

    return uiState.enabled &&
        shouldUseHiddenBackspaceTarget(uiState = uiState) &&
        visibleText.isEmpty() &&
        visibleSelectionStart == visibleSelectionEnd &&
        visibleSelectionStart == 0
}

internal fun shouldRemoveLastRecipientAfterHiddenBackspaceTargetDeleted(
    previousText: String,
    nextText: String,
    uiState: RecipientSelectionQueryFieldUiState,
): Boolean {
    return previousText == BACKSPACE_SENTINEL &&
        nextText.isEmpty() &&
        shouldUseHiddenBackspaceTarget(uiState = uiState)
}
