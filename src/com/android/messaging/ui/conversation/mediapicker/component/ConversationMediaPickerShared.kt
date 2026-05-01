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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val PICKER_CONTROL_BUTTON_SIZE = 48.dp

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
    containerColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f),
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
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
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
            containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            disabledContainerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f),
            disabledContentColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f),
        ),
        shape = CircleShape,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}
