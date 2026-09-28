package com.android.messaging.ui.appsettings.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mms
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn

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
    onClick: () -> Unit,
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
                enabled = enabled,
            )
        },
    )
}

@Composable
internal fun <T> SettingsListItem(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }

    SettingsClickableItem(
        title = title,
        summary = optionLabel(selectedOption),
        onClick = { isDialogVisible = true },
        modifier = modifier,
        icon = icon,
        enabled = enabled,
    )

    if (isDialogVisible) {
        SettingsSingleChoiceDialog(
            title = title,
            options = options,
            selectedOption = selectedOption,
            optionLabel = optionLabel,
            onOptionSelected = { option ->
                onOptionSelected(option)
                isDialogVisible = false
            },
            onDismiss = { isDialogVisible = false },
        )
    }
}

@Composable
private fun <T> SettingsSingleChoiceDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    SettingsChoiceOption(
                        text = optionLabel(option),
                        selected = option == selectedOption,
                        onClick = { onOptionSelected(option) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun SettingsChoiceOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
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

@PreviewLightDark
@Composable
private fun SettingsClickableItemPreview() {
    MessagingPreviewColumn {
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

@PreviewLightDark
@Composable
private fun SettingsCategoryHeaderPreview() {
    MessagingPreviewColumn {
        SettingsCategoryHeader(
            title = "MMS Messaging",
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsSwitchItemPreview() {
    MessagingPreviewColumn {
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

@PreviewLightDark
@Composable
private fun SettingsListItemPreview() {
    MessagingPreviewColumn {
        Column {
            SettingsListItem(
                title = "Swipe left",
                options = listOf("Archive", "Delete", "Mark as read"),
                selectedOption = "Archive",
                optionLabel = { it },
                onOptionSelected = {},
                icon = Icons.Default.Swipe,
            )
            SettingsListItem(
                title = "Disabled option",
                options = listOf("On", "Off"),
                selectedOption = "Off",
                optionLabel = { it },
                onOptionSelected = {},
                enabled = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsSingleChoiceDialogPreview() {
    MessagingPreviewColumn {
        SettingsSingleChoiceDialog(
            title = "Swipe left",
            options = listOf("Archive", "Delete", "Mark as read"),
            selectedOption = "Delete",
            optionLabel = { it },
            onOptionSelected = {},
            onDismiss = {},
        )
    }
}
