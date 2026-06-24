package com.android.messaging.ui.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.ui.core.MessagingPreviewTheme

private val DialogHorizontalPadding = 24.dp

private val SnoozeOption.labelRes: Int
    get() = when (this) {
        SnoozeOption.OneHour -> R.string.snooze_chat_option_one_hour
        SnoozeOption.EightHours -> R.string.snooze_chat_option_eight_hours
        SnoozeOption.TwentyFourHours -> R.string.snooze_chat_option_twenty_four_hours
        SnoozeOption.Always -> R.string.snooze_chat_option_always
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SnoozeChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (SnoozeOption) -> Unit,
) {
    var selectedOption by rememberSaveable { mutableStateOf(SnoozeOption.OneHour) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = DialogHorizontalPadding),
            ) {
                SnoozeChatDialogHeader()

                Column(modifier = Modifier.selectableGroup()) {
                    SnoozeOption.entries.forEach { option ->
                        SnoozeOptionRow(
                            text = stringResource(option.labelRes),
                            selected = option == selectedOption,
                            onClick = { selectedOption = option },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SnoozeChatDialogButtons(
                    onDismiss = onDismiss,
                    onConfirm = { onConfirm(selectedOption) },
                )
            }
        }
    }
}

@Composable
private fun SnoozeChatDialogHeader() {
    Icon(
        imageVector = Icons.Default.Snooze,
        contentDescription = stringResource(R.string.snooze_chat_setting_title),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally),
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.snooze_chat_dialog_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DialogHorizontalPadding),
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = stringResource(R.string.snooze_chat_dialog_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = DialogHorizontalPadding),
    )

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SnoozeChatDialogButtons(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DialogHorizontalPadding),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(android.R.string.cancel))
        }

        TextButton(onClick = onConfirm) {
            Text(text = stringResource(R.string.snooze_chat_dialog_confirm))
        }
    }
}

@Composable
private fun SnoozeOptionRow(
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
            .padding(
                horizontal = DialogHorizontalPadding,
                vertical = 12.dp,
            ),
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

@PreviewLightDark
@Composable
private fun SnoozeChatDialogPreview() {
    MessagingPreviewTheme {
        SnoozeChatDialog(
            onDismiss = {},
            onConfirm = {},
        )
    }
}
