@file:OptIn(ExperimentalComposeUiApi::class)

package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.recipientpicker.model.selection.RecipientSelectionQueryFieldUiState
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.ui.recipientselection.preview.previewSelectedRecipient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun RecipientSelectionQueryField(
    uiState: RecipientSelectionQueryFieldUiState,
    state: TextFieldState,
    onQueryFocusChanged: (Boolean) -> Unit,
    onLastSelectedRecipientRemove: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    maxWidth: Dp? = null,
) {
    BasicTextField(
        modifier = modifier
            .testTag(tag = RECIPIENT_SELECTION_QUERY_FIELD_TEST_TAG)
            .width(
                width = recipientSelectionQueryFieldWidth(
                    uiState = uiState,
                    maxWidth = maxWidth,
                ),
            )
            .focusRequester(focusRequester = focusRequester)
            .onFocusChanged { focusState ->
                onQueryFocusChanged(focusState.isFocused)
            }
            .onPreviewKeyEvent { keyEvent ->
                val shouldRemoveLastRecipient = shouldRemoveLastRecipientFromHardwareBackspace(
                    keyEvent = keyEvent,
                    text = state.text,
                    selection = state.selection,
                    uiState = uiState,
                )

                if (shouldRemoveLastRecipient) {
                    onLastSelectedRecipientRemove()
                }

                shouldRemoveLastRecipient
            },
        state = state,
        enabled = uiState.enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = recipientSelectionQueryTextStyle(uiState = uiState),
        cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary),
        inputTransformation = RecipientSelectionHiddenBackspaceTargetInputTransformation,
        outputTransformation = RecipientSelectionHiddenBackspaceTargetOutputTransformation,
        decorator = TextFieldDecorator { innerTextField ->
            Box(
                modifier = Modifier.heightIn(min = 32.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (uiState.query.isEmpty()) {
                    Text(
                        text = uiState.placeholderText,
                        style = recipientSelectionQueryPlaceholderTextStyle(uiState = uiState),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                innerTextField()
            }
        },
    )
}

@Composable
private fun recipientSelectionQueryFieldWidth(
    uiState: RecipientSelectionQueryFieldUiState,
    maxWidth: Dp?,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = recipientSelectionQueryTextStyle(uiState = uiState)
    val textForMeasurement = recipientSelectionQueryFieldWidthText(uiState = uiState)
    val measuredTextWidth = remember(
        textMeasurer,
        density,
        textStyle,
        textForMeasurement,
    ) {
        with(density) {
            textMeasurer
                .measure(
                    text = AnnotatedString(text = textForMeasurement),
                    style = textStyle,
                    maxLines = 1,
                )
                .size
                .width
                .toDp()
        }
    }

    val desiredWidth = maxOf(48.dp, measuredTextWidth + 4.dp)

    return when {
        maxWidth == null -> desiredWidth
        else -> minOf(desiredWidth, maxWidth)
    }
}

private fun recipientSelectionQueryFieldWidthText(
    uiState: RecipientSelectionQueryFieldUiState,
): String {
    return when {
        uiState.query.isEmpty() -> uiState.placeholderText
        else -> uiState.query
    }
}

@Composable
private fun recipientSelectionQueryTextStyle(
    uiState: RecipientSelectionQueryFieldUiState,
): TextStyle {
    return recipientSelectionQueryPlaceholderTextStyle(uiState = uiState).copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun recipientSelectionQueryPlaceholderTextStyle(
    uiState: RecipientSelectionQueryFieldUiState,
): TextStyle {
    return when {
        uiState.selectedRecipients.isEmpty() -> MaterialTheme.typography.bodyLarge
        else -> {
            MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal)
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionQueryFieldPlaceholderPreview() {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            RecipientSelectionQueryFieldPreviewItem(
                label = "Empty, no selected recipients",
                uiState = previewRecipientSelectionQueryFieldUiState(),
            )
            RecipientSelectionQueryFieldPreviewItem(
                label = "Empty, selected recipient backspace target",
                uiState = previewRecipientSelectionQueryFieldUiState(
                    selectedRecipients = persistentListOf(previewSelectedRecipient()),
                ),
            )
            RecipientSelectionQueryFieldPreviewItem(
                label = "Disabled placeholder",
                uiState = previewRecipientSelectionQueryFieldUiState(
                    enabled = false,
                    selectedRecipients = persistentListOf(previewSelectedRecipient()),
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionQueryFieldQueryPreview() {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            RecipientSelectionQueryFieldPreviewItem(
                label = "Short query",
                uiState = previewRecipientSelectionQueryFieldUiState(query = "Ada"),
            )
            RecipientSelectionQueryFieldPreviewItem(
                label = "Query after selected recipient",
                uiState = previewRecipientSelectionQueryFieldUiState(
                    query = "Grace Hopper",
                    selectedRecipients = persistentListOf(previewSelectedRecipient()),
                ),
            )
            RecipientSelectionQueryFieldPreviewItem(
                label = "Disabled query",
                uiState = previewRecipientSelectionQueryFieldUiState(
                    query = "+31 6 2222 3333",
                    enabled = false,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionQueryFieldLongTextPreview() {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            RecipientSelectionQueryFieldPreviewItem(
                label = "Long query constrained by available row width",
                uiState = previewRecipientSelectionQueryFieldUiState(
                    query = "averylongcontactsearchquery@example.messaging.preview",
                    selectedRecipients = persistentListOf(previewSelectedRecipient()),
                ),
                maxWidth = 152.dp,
            )
            RecipientSelectionQueryFieldPreviewItem(
                label = "Long localized placeholder constrained by row width",
                uiState = previewRecipientSelectionQueryFieldUiState(
                    placeholderText = "Name, phone number, email address, or group alias",
                ),
                maxWidth = 184.dp,
            )
        }
    }
}

@Composable
private fun RecipientSelectionQueryFieldPreviewItem(
    label: String,
    uiState: RecipientSelectionQueryFieldUiState,
    maxWidth: Dp? = null,
) {
    val editableText = recipientSelectionQueryFieldEditableText(uiState = uiState)
    val textFieldState = remember(editableText) {
        TextFieldState(initialText = editableText)
    }
    val focusRequester = remember { FocusRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RecipientSelectionQueryField(
            uiState = uiState,
            state = textFieldState,
            onQueryFocusChanged = { _ -> },
            onLastSelectedRecipientRemove = {},
            focusRequester = focusRequester,
            maxWidth = maxWidth,
        )
    }
}

private fun previewRecipientSelectionQueryFieldUiState(
    query: String = "",
    enabled: Boolean = true,
    placeholderText: String = "Name or phone number",
    selectedRecipients: ImmutableList<SelectedRecipient> = persistentListOf(),
): RecipientSelectionQueryFieldUiState {
    return RecipientSelectionQueryFieldUiState(
        query = query,
        enabled = enabled,
        placeholderText = placeholderText,
        selectedRecipients = selectedRecipients,
    )
}
