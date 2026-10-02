package com.android.messaging.ui.common.components.composer

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.ComposeBarControlHeight
import com.android.messaging.ui.common.components.ComposeBarVerticalPadding
import com.android.messaging.ui.core.MessagingPreviewColumn

private val ComposeBarHorizontalPadding = 12.dp
private val ComposeBarItemSpacing = 8.dp
private val SendButtonSize = 56.dp

// Compose drops IME_FLAG_NO_ENTER_ACTION from a multi-line field once it has an IME action, which
// would turn Enter into Send. Put it back so Enter keeps inserting a new line while voice typing
// and accessibility services can still send, like the legacy actionSend|flagNoEnterAction.
private val EnterInsertsNewLineInterceptor = PlatformTextInputInterceptor { request, nextHandler ->
    nextHandler.startInputMethod(
        request = { outAttributes ->
            request.createInputConnection(outAttributes).also {
                outAttributes.imeOptions =
                    outAttributes.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
            }
        },
    )
}

@Composable
internal fun MessageComposeBar(
    text: String,
    onTextChange: (String) -> Unit,
    isFieldEnabled: Boolean,
    isFieldContentHidden: Boolean,
    fieldFocusRequester: FocusRequester?,
    fieldStateDescription: String?,
    fieldTestTag: String,
    sendAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    attachmentsContent: (@Composable () -> Unit)? = null,
    topContent: (@Composable ColumnScope.() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    fieldOverlay: (@Composable BoxScope.() -> Unit)? = null,
    onImeSend: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        attachmentsContent?.invoke()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ComposeBarHorizontalPadding,
                    vertical = ComposeBarVerticalPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(space = ComposeBarItemSpacing),
            verticalAlignment = Alignment.Bottom,
        ) {
            MessageComposeField(
                modifier = Modifier.weight(weight = 1f),
                text = text,
                onTextChange = onTextChange,
                isEnabled = isFieldEnabled,
                isContentHidden = isFieldContentHidden,
                focusRequester = fieldFocusRequester,
                stateDescription = fieldStateDescription,
                testTag = fieldTestTag,
                topContent = topContent,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                overlay = fieldOverlay,
                onImeSend = onImeSend,
            )

            sendAction()
        }
    }
}

@Composable
private fun MessageComposeField(
    text: String,
    onTextChange: (String) -> Unit,
    isEnabled: Boolean,
    isContentHidden: Boolean,
    focusRequester: FocusRequester?,
    stateDescription: String?,
    testTag: String,
    topContent: (@Composable ColumnScope.() -> Unit)?,
    leadingContent: (@Composable () -> Unit)?,
    trailingContent: (@Composable () -> Unit)?,
    overlay: (@Composable BoxScope.() -> Unit)?,
    onImeSend: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val focusRequesterModifier = focusRequester
        ?.let(Modifier::focusRequester)
        ?: Modifier

    val contentHiddenModifier = when {
        isContentHidden -> {
            Modifier
                .alpha(alpha = 0f)
                .clearAndSetSemantics {}
        }

        else -> Modifier
    }

    val stateDescriptionModifier = when (stateDescription) {
        null -> Modifier
        else -> Modifier.semantics {
            this.stateDescription = stateDescription
        }
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            topContent?.invoke(this)

            Box {
                InterceptPlatformTextInput(interceptor = EnterInsertsNewLineInterceptor) {
                    MessageComposeTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(focusRequesterModifier)
                            .testTag(testTag)
                            .heightIn(min = ComposeBarControlHeight)
                            .then(stateDescriptionModifier)
                            .then(contentHiddenModifier),
                        text = text,
                        onTextChange = onTextChange,
                        isEnabled = isEnabled,
                        leadingContent = leadingContent,
                        trailingContent = trailingContent,
                        onImeSend = onImeSend,
                    )
                }

                overlay?.invoke(this)
            }
        }
    }
}

@Composable
private fun MessageComposeTextField(
    text: String,
    onTextChange: (String) -> Unit,
    isEnabled: Boolean,
    leadingContent: (@Composable () -> Unit)?,
    trailingContent: (@Composable () -> Unit)?,
    onImeSend: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    TextField(
        modifier = modifier,
        value = text,
        onValueChange = onTextChange,
        enabled = isEnabled,
        shape = MaterialTheme.shapes.large,
        colors = messageComposeFieldColors(),
        placeholder = ::MessageComposePlaceholder,
        leadingIcon = leadingContent,
        trailingIcon = trailingContent,
        minLines = 1,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.ShortMessage,
            imeAction = when (onImeSend) {
                null -> ImeAction.Default
                else -> ImeAction.Send
            },
        ),
        keyboardActions = KeyboardActions(onSend = { onImeSend?.invoke() }),
    )
}

@Composable
private fun MessageComposePlaceholder() {
    Text(
        text = stringResource(id = R.string.compose_message_view_hint_text),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun messageComposeFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun MessageSendButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val contentColor = when {
        enabled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .size(size = SendButtonSize)
            .testTag(MESSAGE_SEND_BUTTON_TEST_TAG),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = stringResource(id = R.string.sendButtonContentDescription),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(id = R.string.sendButtonContentDescription),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MessageComposeBarPreview() {
    MessagingPreviewColumn {
        MessageComposeBar(
            text = "",
            onTextChange = {},
            isFieldEnabled = true,
            isFieldContentHidden = false,
            fieldFocusRequester = null,
            fieldStateDescription = null,
            fieldTestTag = MESSAGE_COMPOSE_FIELD_TEST_TAG,
            sendAction = {
                MessageSendButton(
                    enabled = true,
                    onClick = {},
                )
            },
        )
    }
}
