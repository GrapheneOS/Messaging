package com.android.messaging.ui.conversation.recipientpicker.component

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryCardUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryChipsUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryFieldUiState
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryTextUiState
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionContentUiState
import com.android.messaging.ui.recipientselection.model.selection.RecipientSelectionStrings
import com.android.messaging.ui.recipientselection.preview.previewRecipientSelectionContentUiState
import kotlinx.collections.immutable.ImmutableList

private val recipientSelectionInputRowMinHeight = 32.dp

internal fun recipientSelectionQueryCardUiState(
    uiState: RecipientSelectionContentUiState,
    strings: RecipientSelectionStrings,
    armedRecipientDestination: String?,
): RecipientSelectionQueryCardUiState {
    return RecipientSelectionQueryCardUiState(
        text = RecipientSelectionQueryTextUiState(
            query = uiState.picker.query,
            enabled = uiState.isQueryEnabled,
            prefixText = strings.queryPrefixText,
            placeholderText = strings.queryPlaceholderText,
        ),
        chips = RecipientSelectionQueryChipsUiState(
            recipients = uiState.selectedRecipients,
            armedRecipientDestination = armedRecipientDestination,
            enabled = recipientSelectionMutationsEnabled(uiState = uiState),
        ),
    )
}

@Composable
internal fun RecipientSelectionQueryCard(
    uiState: RecipientSelectionQueryCardUiState,
    onQueryChanged: (String) -> Unit,
    onQueryFocused: () -> Unit,
    onSelectedRecipientClick: (SelectedRecipient) -> Unit,
    onSelectedRecipientBackspace: (SelectedRecipient) -> Unit,
    focusRequester: FocusRequester,
    simSelectorSlot: (@Composable () -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            RecipientSelectionQueryCardBody(
                uiState = uiState,
                onQueryChanged = onQueryChanged,
                onQueryFocused = onQueryFocused,
                onSelectedRecipientClick = onSelectedRecipientClick,
                onSelectedRecipientBackspace = onSelectedRecipientBackspace,
                focusRequester = focusRequester,
            )
            simSelectorSlot?.invoke()
        }
    }
}

@Composable
private fun RecipientSelectionQueryCardBody(
    uiState: RecipientSelectionQueryCardUiState,
    onQueryChanged: (String) -> Unit,
    onQueryFocused: () -> Unit,
    onSelectedRecipientClick: (SelectedRecipient) -> Unit,
    onSelectedRecipientBackspace: (SelectedRecipient) -> Unit,
    focusRequester: FocusRequester,
) {
    val queryFieldUiState = queryFieldUiState(uiState = uiState)
    val editableText = recipientSelectionQueryFieldEditableText(uiState = queryFieldUiState)
    val textFieldState = rememberTextFieldState(initialText = editableText)

    RecipientSelectionQueryFieldEditableTextReconcileEffect(
        textFieldState = textFieldState,
        editableText = editableText,
        queryFieldUiState = queryFieldUiState,
    )
    RecipientSelectionQueryFieldStateObservationEffect(
        textFieldState = textFieldState,
        queryFieldUiState = queryFieldUiState,
        onQueryChanged = onQueryChanged,
        onSelectedRecipientBackspace = onSelectedRecipientBackspace,
    )

    RecipientSelectionInputRow(
        prefixText = uiState.text.prefixText,
        recipients = uiState.chips.recipients,
        armedRecipientDestination = uiState.chips.armedRecipientDestination,
        chipsEnabled = uiState.chips.enabled,
        queryFieldUiState = queryFieldUiState,
        textFieldState = textFieldState,
        onQueryFocused = onQueryFocused,
        onSelectedRecipientClick = onSelectedRecipientClick,
        onLastSelectedRecipientRemove = {
            uiState.chips.recipients.lastOrNull()?.let(onSelectedRecipientBackspace)
        },
        focusRequester = focusRequester,
    )
}

@Composable
private fun RecipientSelectionQueryFieldEditableTextReconcileEffect(
    textFieldState: TextFieldState,
    editableText: String,
    queryFieldUiState: RecipientSelectionQueryFieldUiState,
) {
    // Soft-IME backspace deletes the sentinel from the buffer without changing editableText when
    // chips remain; re-key on the chip list so the reconcile re-fires and re-installs the sentinel

    LaunchedEffect(editableText, queryFieldUiState.selectedRecipients) {
        if (textFieldState.text.toString() != editableText) {
            textFieldState.edit {
                replace(0, length, editableText)
                placeCursorAtEnd()
            }
        }
    }
}

@Composable
private fun RecipientSelectionQueryFieldStateObservationEffect(
    textFieldState: TextFieldState,
    queryFieldUiState: RecipientSelectionQueryFieldUiState,
    onQueryChanged: (String) -> Unit,
    onSelectedRecipientBackspace: (SelectedRecipient) -> Unit,
) {
    val currentQueryFieldUiState = rememberUpdatedState(newValue = queryFieldUiState)
    val currentOnQueryChanged = rememberUpdatedState(newValue = onQueryChanged)
    val currentOnSelectedRecipientBackspace = rememberUpdatedState(
        newValue = onSelectedRecipientBackspace,
    )

    LaunchedEffect(textFieldState) {
        var previousText = textFieldState.text.toString()
        snapshotFlow { textFieldState.text.toString() }.collect { currentText ->
            handleRecipientSelectionTextFieldStateChange(
                previousText = previousText,
                currentText = currentText,
                uiState = currentQueryFieldUiState.value,
                onQueryChanged = currentOnQueryChanged.value,
                onSelectedRecipientBackspace = currentOnSelectedRecipientBackspace.value,
            )
            previousText = currentText
        }
    }
}

private fun handleRecipientSelectionTextFieldStateChange(
    previousText: String,
    currentText: String,
    uiState: RecipientSelectionQueryFieldUiState,
    onQueryChanged: (String) -> Unit,
    onSelectedRecipientBackspace: (SelectedRecipient) -> Unit,
) {
    val shouldRemoveLastRecipient = shouldRemoveLastRecipientAfterHiddenBackspaceTargetDeleted(
        previousText = previousText,
        nextText = currentText,
        uiState = uiState,
    )

    when {
        shouldRemoveLastRecipient -> {
            uiState.selectedRecipients.lastOrNull()?.let { recipient ->
                onSelectedRecipientBackspace(recipient)
            }
        }

        else -> {
            val visibleQuery = recipientSelectionVisibleQueryText(fieldText = currentText)
            if (visibleQuery != uiState.query) {
                onQueryChanged(visibleQuery)
            }
        }
    }
}

@Composable
private fun RecipientSelectionInputRow(
    prefixText: String,
    recipients: ImmutableList<SelectedRecipient>,
    armedRecipientDestination: String?,
    chipsEnabled: Boolean,
    queryFieldUiState: RecipientSelectionQueryFieldUiState,
    textFieldState: TextFieldState,
    onQueryFocused: () -> Unit,
    onSelectedRecipientClick: (SelectedRecipient) -> Unit,
    onLastSelectedRecipientRemove: () -> Unit,
    focusRequester: FocusRequester,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentOnQueryFocused = rememberUpdatedState(newValue = onQueryFocused)
    val currentKeyboardController = rememberUpdatedState(newValue = keyboardController)
    val onInputAreaTap: () -> Unit = remember(focusRequester) {
        {
            currentOnQueryFocused.value()
            focusRequester.requestFocus()
            currentKeyboardController.value?.show()
        }
    }
    val outerPadding = recipientSelectionInputRowPadding(hasChips = recipients.isNotEmpty())

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .recipientSelectionFocusQueryOnUnhandledTap(
                enabled = queryFieldUiState.enabled,
                onTap = onInputAreaTap,
            )
            .padding(paddingValues = outerPadding),
    ) {
        RecipientSelectionSelectedRecipientChips(
            recipients = recipients,
            armedRecipientDestination = armedRecipientDestination,
            enabled = chipsEnabled,
            onRecipientClick = onSelectedRecipientClick,
            leadingContent = {
                Text(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .height(height = recipientSelectionInputRowMinHeight)
                        .wrapContentHeight(align = Alignment.CenterVertically),
                    text = prefixText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = { chipModifier ->
                RecipientSelectionQueryField(
                    modifier = chipModifier,
                    uiState = queryFieldUiState,
                    state = textFieldState,
                    onQueryFocusChanged = { isFocused ->
                        if (isFocused) currentOnQueryFocused.value()
                    },
                    onLastSelectedRecipientRemove = onLastSelectedRecipientRemove,
                    focusRequester = focusRequester,
                    maxWidth = maxWidth,
                )
            },
        )
    }
}

private fun recipientSelectionInputRowPadding(hasChips: Boolean): PaddingValues {
    return when {
        hasChips -> {
            PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        }
        else -> {
            PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        }
    }
}

private fun Modifier.recipientSelectionFocusQueryOnUnhandledTap(
    enabled: Boolean,
    onTap: () -> Unit,
): Modifier {
    return when {
        !enabled -> this

        else -> {
            pointerInput(key1 = Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Final,
                    )

                    if (down.isConsumed) {
                        return@awaitEachGesture
                    }

                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)

                    if (up != null && !up.isConsumed) {
                        onTap()
                    }
                }
            }
        }
    }
}

private fun queryFieldUiState(
    uiState: RecipientSelectionQueryCardUiState,
): RecipientSelectionQueryFieldUiState {
    return RecipientSelectionQueryFieldUiState(
        query = uiState.text.query,
        enabled = uiState.text.enabled,
        placeholderText = uiState.text.placeholderText,
        selectedRecipients = uiState.chips.recipients,
    )
}

private fun recipientSelectionMutationsEnabled(
    uiState: RecipientSelectionContentUiState,
): Boolean {
    return uiState.isQueryEnabled && uiState.primaryAction?.isLoading != true
}

@SuppressLint("RememberInComposition")
@PreviewLightDark
@Composable
private fun RecipientSelectionQueryCardPreview() {
    val uiState = recipientSelectionQueryCardUiState(
        uiState = previewRecipientSelectionContentUiState(),
        strings = RecipientSelectionStrings(
            queryPrefixText = "To",
            queryPlaceholderText = "Name or phone number",
        ),
        armedRecipientDestination = "+31622223333",
    )
    MessagingPreviewColumn {
        RecipientSelectionQueryCard(
            uiState = uiState,
            onQueryChanged = { _ -> },
            onQueryFocused = {},
            onSelectedRecipientClick = { _ -> },
            onSelectedRecipientBackspace = { _ -> },
            focusRequester = FocusRequester(),
            simSelectorSlot = null,
        )
    }
}
