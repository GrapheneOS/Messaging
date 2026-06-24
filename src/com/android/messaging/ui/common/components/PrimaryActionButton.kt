package com.android.messaging.ui.common.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn

private val PrimaryActionButtonShape = RoundedCornerShape(size = 16.dp)

private val IconLabelSpacing = 8.dp

private val LoadingIndicatorSize = 24.dp

private const val DISABLED_CONTAINER_ALPHA = 0.1f

private const val DISABLED_CONTENT_ALPHA = 0.4f

@Composable
internal fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    testTag: String? = null,
    shape: Shape = PrimaryActionButtonShape,
) {
    val isInteractionEnabled = enabled && !isLoading
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when {
        enabled -> colorScheme.primaryContainer
        else -> colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    }
    val contentColor = when {
        enabled -> colorScheme.onPrimaryContainer
        else -> colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    }

    ExtendedFloatingActionButton(
        modifier = modifier
            .optionalTestTag(testTag)
            .semantics {
                if (!isInteractionEnabled) {
                    disabled()
                }
            },
        onClick = {
            if (isInteractionEnabled) {
                onClick()
            }
        },
        expanded = expanded,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        icon = {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size = LoadingIndicatorSize),
                        color = contentColor,
                        strokeWidth = 2.dp,
                    )
                }

                leadingIcon != null -> {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = if (expanded) null else text,
                    )
                }
            }
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = text)

                trailingIcon?.let { icon ->
                    Spacer(modifier = Modifier.size(size = IconLabelSpacing))

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun PrimaryActionButtonPreview() {
    MessagingPreviewColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryActionButton(
                text = "Start chat",
                onClick = {},
                leadingIcon = Icons.AutoMirrored.Rounded.Chat,
            )

            Spacer(modifier = Modifier.size(size = 12.dp))

            PrimaryActionButton(
                text = "Start chat",
                onClick = {},
                isLoading = true,
                expanded = false,
                leadingIcon = Icons.AutoMirrored.Rounded.Chat,
            )

            Spacer(modifier = Modifier.size(size = 12.dp))

            PrimaryActionButton(
                text = "Start chat",
                onClick = {},
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}
