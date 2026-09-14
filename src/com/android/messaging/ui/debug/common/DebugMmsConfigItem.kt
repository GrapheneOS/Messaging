package com.android.messaging.ui.debug.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.debug.screen.model.MmsConfigItemUiState

@Composable
internal fun DebugMmsConfigItem(
    item: MmsConfigItemUiState,
    onToggle: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is MmsConfigItemUiState.Toggle -> {
            ListItem(
                headlineContent = { Text(text = item.key) },
                modifier = modifier.toggleable(
                    value = item.checked,
                    role = Role.Switch,
                    onValueChange = onToggle,
                ),
                trailingContent = {
                    Switch(
                        checked = item.checked,
                        onCheckedChange = null,
                    )
                },
            )
        }

        is MmsConfigItemUiState.Editable -> {
            ListItem(
                headlineContent = { Text(text = item.key) },
                modifier = modifier.clickable(onClick = onEditClick),
                supportingContent = { EditableValueText(value = item.value) },
            )
        }
    }
}

@Composable
private fun EditableValueText(value: String) {
    val isEmpty = value.isBlank()
    val text = when {
        isEmpty -> stringResource(R.string.debug_mms_config_empty_value)
        else -> value
    }

    Text(
        text = text,
        fontStyle = when {
            isEmpty -> FontStyle.Italic
            else -> null
        },
    )
}

@PreviewLightDark
@Composable
private fun DebugMmsConfigItemPreview() {
    MessagingPreviewColumn {
        DebugMmsConfigItem(
            item = MmsConfigItemUiState.Toggle(
                key = "enabledMMS",
                checked = true,
            ),
            onToggle = {},
            onEditClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DebugMmsConfigEditableItemPreview() {
    MessagingPreviewColumn {
        DebugMmsConfigItem(
            item = MmsConfigItemUiState.Editable(
                key = "maxMessageSize",
                value = "1048576",
                isNumeric = true,
            ),
            onToggle = {},
            onEditClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DebugMmsConfigEmptyValuePreview() {
    MessagingPreviewColumn {
        DebugMmsConfigItem(
            item = MmsConfigItemUiState.Editable(
                key = "userAgent",
                value = "",
                isNumeric = false,
            ),
            onToggle = {},
            onEditClick = {},
        )
    }
}
