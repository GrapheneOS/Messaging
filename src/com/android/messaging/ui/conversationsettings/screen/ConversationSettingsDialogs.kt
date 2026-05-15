package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.notification.SnoozeOption
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State

private val DialogHorizontalPadding = 24.dp

private val SnoozeOption.labelRes: Int
    get() = when (this) {
        SnoozeOption.OneHour -> R.string.snooze_chat_option_one_hour
        SnoozeOption.EightHours -> R.string.snooze_chat_option_eight_hours
        SnoozeOption.TwentyFourHours -> R.string.snooze_chat_option_twenty_four_hours
        SnoozeOption.Always -> R.string.snooze_chat_option_always
    }

@Composable
internal fun ConversationSettingsDialogs(
    uiState: State,
    onAction: (Action) -> Unit,
    pendingBlockConfirmation: Boolean,
    showSnoozeChatDialog: Boolean,
    onDismissBlockConfirmation: () -> Unit,
    onDismissSnoozeChat: () -> Unit,
) {
    if (pendingBlockConfirmation) {
        val displayName = uiState.otherParticipant?.displayDestination.orEmpty()

        BlockConfirmationDialog(
            displayName = displayName,
            onDismiss = onDismissBlockConfirmation,
            onConfirm = {
                onAction(Action.BlockConfirmed)
                onDismissBlockConfirmation()
            },
        )
    }

    if (showSnoozeChatDialog) {
        SnoozeChatDialog(
            onDismiss = onDismissSnoozeChat,
            onConfirm = { option ->
                onAction(Action.SnoozeOptionSelected(option))
                onDismissSnoozeChat()
            },
        )
    }
}

@Composable
private fun BlockConfirmationDialog(
    displayName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.block_confirmation_title, displayName))
        },
        text = {
            Text(text = stringResource(R.string.block_confirmation_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnoozeChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (SnoozeOption) -> Unit,
) {
    var selectedOption by rememberSaveable { mutableStateOf(SnoozeOption.OneHour) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(vertical = DialogHorizontalPadding)) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DialogHorizontalPadding),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                    TextButton(onClick = { onConfirm(selectedOption) }) {
                        Text(text = stringResource(R.string.snooze_chat_dialog_confirm))
                    }
                }
            }
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

@Preview
@Composable
private fun BlockConfirmationDialogPreview() {
    AppTheme {
        BlockConfirmationDialog(
            displayName = "+31 6 1234 5678",
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun SnoozeChatDialogPreview() {
    AppTheme {
        SnoozeChatDialog(
            onDismiss = {},
            onConfirm = {},
        )
    }
}
