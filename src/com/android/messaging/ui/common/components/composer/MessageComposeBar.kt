package com.android.messaging.ui.common.components.composer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.ComposeBarControlHeight
import com.android.messaging.ui.common.components.ComposeBarVerticalPadding
import com.android.messaging.ui.core.MessagingPreviewColumn

private val ComposeBarHorizontalPadding = 12.dp
private val ComposeBarItemSpacing = 8.dp
private val SendButtonSize = 56.dp
private val FieldIconMinSize = 48.dp

/**
 * @param text the message text the field's owner holds. The field reports each edit through
 * [onTextChange] and keeps showing its own text until the owner replaces it.
 * @param textRevision the owner moves this whenever it replaces [text] itself, for example
 * clearing it after a send. The field only takes [text] when this changes, because the owner
 * catches up with edits asynchronously and can hand back an older edit or one equal to the text
 * it's replacing.
 * @param onTextChange called with each edit and the [textRevision] it was typed over. An owner
 * whose revision has moved on since should drop the edit: it was typed over text the owner has
 * replaced, and the field takes the owner's new text once it arrives.
 */
@Composable
internal fun MessageComposeBar(
    text: String,
    textRevision: Int,
    onTextChange: (text: String, textRevision: Int) -> Unit,
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
                textRevision = textRevision,
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
    textRevision: Int,
    onTextChange: (text: String, textRevision: Int) -> Unit,
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

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            topContent?.invoke(this)

            Box {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(testTag)
                            .heightIn(min = ComposeBarControlHeight)
                            .then(contentHiddenModifier),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        leadingContent?.let { MessageComposeFieldIcon(content = it) }

                        MessageComposeTextField(
                            modifier = Modifier
                                .weight(weight = 1f)
                                .then(focusRequesterModifier),
                            text = text,
                            textRevision = textRevision,
                            onTextChange = onTextChange,
                            isEnabled = isEnabled,
                            isContentHidden = isContentHidden,
                            stateDescription = stateDescription,
                            hasLeadingContent = leadingContent != null,
                            hasTrailingContent = trailingContent != null,
                            onImeSend = onImeSend,
                        )

                        trailingContent?.let { MessageComposeFieldIcon(content = it) }
                    }
                }

                overlay?.invoke(this)
            }
        }
    }
}

@Composable
private fun MessageComposeFieldIcon(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.defaultMinSize(
            minWidth = FieldIconMinSize,
            minHeight = FieldIconMinSize,
        ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
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
            textRevision = 0,
            onTextChange = { _, _ -> },
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
