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
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
private val recipientSelectionInputRowPadding = PaddingValues(
    horizontal = 16.dp,
    vertical = 12.dp,
)

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
    onKeyboardAction: KeyboardActionHandler? = null,
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
                onKeyboardAction = onKeyboardAction,
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
    onKeyboardAction: KeyboardActionHandler?,
) {
    val queryFieldUiState = queryFieldUiState(uiState = uiState)
    val editableText = recipientSelectionQueryFieldEditableText(uiState = queryFieldUiState)
    val textFieldState = rememberTextFieldState(initialText = editableText)
    val sentQueries = remember {
        RecipientSelectionSentQueries(initialQuery = queryFieldUiState.query)
    }

    RecipientSelectionQueryFieldEditableTextReconcileEffect(
        textFieldState = textFieldState,
        editableText = editableText,
        queryFieldUiState = queryFieldUiState,
        sentQueries = sentQueries,
    )
    RecipientSelectionQueryFieldStateObservationEffect(
        textFieldState = textFieldState,
        queryFieldUiState = queryFieldUiState,
        sentQueries = sentQueries,
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
        onKeyboardAction = onKeyboardAction,
    )
}

@Stable
internal class RecipientSelectionSentQueries(
    initialQuery: String,
) {
    private val pendingQueries = ArrayDeque<String>()
    private var latestQuery = initialQuery

    fun trySend(query: String): Boolean {
        if (query == latestQuery) {
            return false
        }

        pendingQueries.addLast(query)
        latestQuery = query
        return true
    }

    fun consumeEcho(stateQuery: String): Boolean {
        val index = pendingQueries.indexOf(stateQuery)

        if (index < 0) {
            acceptStateQuery(stateQuery = stateQuery)
            return false
        }

        repeat(times = index + 1) { pendingQueries.removeFirst() }
        return true
    }

    fun acceptStateQuery(stateQuery: String) {
        pendingQueries.clear()
        latestQuery = stateQuery
    }
}

@Composable
private fun RecipientSelectionQueryFieldEditableTextReconcileEffect(
    textFieldState: TextFieldState,
    editableText: String,
    queryFieldUiState: RecipientSelectionQueryFieldUiState,
    sentQueries: RecipientSelectionSentQueries,
) {
    LaunchedEffect(queryFieldUiState.selectedRecipients) {
        sentQueries.acceptStateQuery(stateQuery = queryFieldUiState.query)
        textFieldState.replaceTextIfDifferent(text = editableText)
    }

    LaunchedEffect(editableText) {
        val isEcho = sentQueries.consumeEcho(stateQuery = queryFieldUiState.query)
        val visibleQuery = recipientSelectionVisibleQueryText(
            fieldText = textFieldState.text.toString(),
        )

        if (!isEcho || visibleQuery == queryFieldUiState.query) {
            textFieldState.replaceTextIfDifferent(text = editableText)
        }
    }
}

private fun TextFieldState.replaceTextIfDifferent(text: String) {
    if (this.text.toString() != text) {
        edit {
            replace(0, length, text)
            placeCursorAtEnd()
        }
    }
}

@Composable
private fun RecipientSelectionQueryFieldStateObservationEffect(
    textFieldState: TextFieldState,
    queryFieldUiState: RecipientSelectionQueryFieldUiState,
    sentQueries: RecipientSelectionSentQueries,
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
                sentQueries = sentQueries,
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
    sentQueries: RecipientSelectionSentQueries,
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
            // Compared with the latest send, not the state, which can still hold an older query
            if (sentQueries.trySend(query = visibleQuery)) {
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
    onKeyboardAction: KeyboardActionHandler?,
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .recipientSelectionFocusQueryOnUnhandledTap(
                enabled = queryFieldUiState.enabled,
                onTap = onInputAreaTap,
            )
            .padding(paddingValues = recipientSelectionInputRowPadding),
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
                    // Translations of the label end with a space; the end padding already separates it
                    text = prefixText.trim(),
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
                    onKeyboardAction = onKeyboardAction,
                )
            },
        )
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
