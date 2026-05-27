package com.android.messaging.ui.conversation.mediapicker.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn

private val PICKER_CONTROL_BUTTON_SIZE = 48.dp

internal fun pickerOverlayContainerColor(alpha: Float): Color {
    return Color.Black.copy(alpha = alpha)
}

internal fun pickerOverlayContentColor(alpha: Float = 1f): Color {
    return Color.White.copy(alpha = alpha)
}

@Composable
internal fun PermissionFallback(
    icon: @Composable () -> Unit,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier
                        .padding(all = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .heightIn(min = 56.dp),
                onClick = onActionClick,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
internal fun PickerOverlayBackgroundButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = PICKER_CONTROL_BUTTON_SIZE,
    containerColor: Color = pickerOverlayContainerColor(alpha = 0.48f),
    contentDescription: String,
    iconSize: Dp = 24.dp,
    imageVector: ImageVector,
    onClick: () -> Unit,
) {
    FilledIconButton(
        modifier = modifier
            .size(buttonSize),
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = pickerOverlayContentColor(),
        ),
    ) {
        Icon(
            modifier = Modifier
                .size(iconSize),
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@Composable
internal fun PickerOverlayIconButton(
    modifier: Modifier = Modifier,
    contentDescription: String,
    enabled: Boolean = true,
    imageVector: ImageVector,
    onClick: () -> Unit,
) {
    FilledIconButton(
        modifier = modifier
            .size(PICKER_CONTROL_BUTTON_SIZE),
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = pickerOverlayContainerColor(alpha = 0.5f),
            contentColor = pickerOverlayContentColor(),
            disabledContainerColor = pickerOverlayContainerColor(alpha = 0.25f),
            disabledContentColor = pickerOverlayContentColor(alpha = 0.5f),
        ),
        shape = CircleShape,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationMediaPickerSharedPreview() {
    MessagingPreviewColumn {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PermissionFallback(
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                    )
                },
                message = "Allow camera access to take photos and videos.",
                actionLabel = "Allow camera",
                onActionClick = {},
            )
            Spacer(modifier = Modifier.size(size = 12.dp))
            PickerOverlayIconButton(
                contentDescription = "Close",
                imageVector = Icons.Rounded.Close,
                onClick = {},
            )
        }
    }
}
