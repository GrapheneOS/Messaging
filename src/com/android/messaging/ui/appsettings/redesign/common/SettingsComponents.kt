package com.android.messaging.ui.appsettings.redesign.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mms
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.AppTheme

private const val DISABLED_ALPHA = 0.38f

@Composable
private fun contentColor(
    enabled: Boolean,
    color: Color,
): Color {
    return when {
        enabled -> color
        else -> color.copy(alpha = DISABLED_ALPHA)
    }
}

@Composable
internal fun SettingsClickableItem(
    title: String,
    onClick: (() -> Unit),
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingsItemLayout(
        title = title,
        modifier = modifier.clickable(
            enabled = enabled,
            onClick = onClick,
        ),
        summary = summary,
        icon = icon,
        enabled = enabled,
        verticalPadding = 16.dp,
    )
}

@Composable
internal fun SettingsCategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 8.dp,
            ),
    )
}

@Composable
internal fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    SettingsItemLayout(
        title = title,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        summary = summary,
        icon = icon,
        enabled = enabled,
        verticalPadding = 12.dp,
        trailing = {
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
    )
}

@Composable
private fun SettingsItemLayout(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    verticalPadding: Dp = 16.dp,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor(
                    enabled = enabled,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor(
                    enabled = enabled,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
            if (!summary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor(
                        enabled = enabled,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        trailing?.invoke()
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsClickableItemPreview() {
    AppTheme {
        Column {
            SettingsClickableItem(
                title = "Language",
                summary = "English",
                icon = Icons.Default.Language,
                onClick = {},
            )
            SettingsClickableItem(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                onClick = {},
            )
            SettingsClickableItem(
                title = "About",
                summary = "Version 1.0.0",
                onClick = {},
            )
            SettingsClickableItem(
                title = "Disabled item",
                summary = "Not available",
                icon = Icons.Default.Lock,
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsCategoryHeaderPreview() {
    AppTheme {
        SettingsCategoryHeader(
            title = "MMS Messaging",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsSwitchItemPreview() {
    AppTheme {
        Column {
            SettingsSwitchItem(
                title = "Auto-retrieve MMS",
                summary = "Automatically retrieve messages",
                icon = Icons.Default.Mms,
                checked = true,
                onCheckedChange = {},
            )
            SettingsSwitchItem(
                title = "Delivery reports",
                summary = "Request a delivery report",
                checked = false,
                onCheckedChange = {},
            )
            SettingsSwitchItem(
                title = "Disabled option",
                summary = "Not available",
                icon = Icons.Default.Block,
                checked = false,
                enabled = false,
                onCheckedChange = {},
            )
        }
    }
}
