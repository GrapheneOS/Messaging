package com.android.messaging.ui.conversation.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG
import com.android.messaging.ui.conversation.screen.model.ConversationAttachmentLimitWarning
import com.android.messaging.ui.conversation.screen.model.ConversationMessageDeleteConfirmationUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState

@Composable
internal fun ConversationScreenDialogs(
    uiState: ConversationScreenScaffoldUiState,
    screenModel: ConversationScreenModel,
) {
    uiState.attachmentLimitWarning?.let { warning ->
        ConversationAttachmentLimitWarningDialog(
            warning = warning,
            onDismiss = screenModel::dismissAttachmentLimitWarning,
            onSendAnyway = screenModel::sendAnywayAfterAttachmentLimitWarning,
        )
    }

    uiState.selection.deleteConfirmation?.let { deleteConfirmation ->
        ConversationDeleteMessagesDialog(
            deleteConfirmation = deleteConfirmation,
            onConfirm = screenModel::confirmDeleteSelectedMessages,
            onDismiss = screenModel::dismissDeleteMessageConfirmation,
        )
    }

    if (uiState.isDeleteConversationConfirmationVisible) {
        ConversationDeleteConversationDialog(
            onConfirm = screenModel::confirmDeleteConversation,
            onDismiss = screenModel::dismissDeleteConversationConfirmation,
        )
    }

    if (uiState.isSubjectDialogVisible) {
        ConversationSubjectFieldDialog(
            initialSubjectText = uiState.composer.subjectText,
            onConfirm = screenModel::onSubjectDialogConfirm,
            onDismiss = screenModel::onSubjectDialogDismiss,
        )
    }
}

@Composable
private fun ConversationAttachmentLimitWarningDialog(
    warning: ConversationAttachmentLimitWarning,
    onDismiss: () -> Unit,
    onSendAnyway: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.mms_attachment_limit_reached),
            )
        },
        text = {
            Text(
                text = stringResource(
                    id = when (warning) {
                        ConversationAttachmentLimitWarning.ComposingAttachmentLimitReached -> {
                            R.string.attachment_limit_reached_dialog_message_when_composing
                        }

                        ConversationAttachmentLimitWarning.SendingMessageLimitReached -> {
                            R.string.attachment_limit_reached_dialog_message_when_sending
                        }

                        ConversationAttachmentLimitWarning.SendingVideoAttachmentLimitReached -> {
                            R.string.video_attachment_limit_exceeded_when_sending
                        }
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(android.R.string.ok),
                )
            }
        },
        dismissButton = when (warning) {
            ConversationAttachmentLimitWarning.SendingMessageLimitReached -> {
                {
                    TextButton(onClick = onSendAnyway) {
                        Text(
                            text = stringResource(R.string.attachment_limit_reached_send_anyway),
                        )
                    }
                }
            }

            ConversationAttachmentLimitWarning.ComposingAttachmentLimitReached,
            ConversationAttachmentLimitWarning.SendingVideoAttachmentLimitReached,
            -> null
        },
    )
}

@Composable
private fun ConversationDeleteConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.delete_conversations_confirmation_dialog_title,
                    count = 1,
                    1,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete_conversation_confirmation_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.delete_conversation_decline_button))
            }
        },
    )
}

@Composable
private fun ConversationSubjectFieldDialog(
    initialSubjectText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var fieldText by remember(initialSubjectText) {
        mutableStateOf(
            value = TextFieldValue(
                text = initialSubjectText,
                selection = TextRange(index = initialSubjectText.length),
            ),
        )
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = Modifier.testTag(tag = CONVERSATION_SUBJECT_DIALOG_TEST_TAG),
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.subject_dialog_title))
        },
        text = {
            ConversationSubjectFieldInput(
                value = fieldText,
                onValueChange = { newValue -> fieldText = newValue },
                focusRequester = focusRequester,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fieldText.text) }) {
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

@Composable
private fun ConversationSubjectFieldInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester = focusRequester)
            .testTag(CONVERSATION_SUBJECT_DIALOG_TEXT_FIELD_TEST_TAG),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.compose_message_view_subject_hint_text),
            )
        },
        trailingIcon = {
            if (value.text.isNotEmpty()) {
                IconButton(
                    modifier = Modifier
                        .testTag(CONVERSATION_SUBJECT_DIALOG_CLEAR_BUTTON_TEST_TAG),
                    onClick = { onValueChange(TextFieldValue(text = "")) },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cancel,
                        contentDescription = stringResource(
                            id = R.string.delete_subject_content_description,
                        ),
                    )
                }
            }
        },
    )
}

@Composable
private fun ConversationDeleteMessagesDialog(
    deleteConfirmation: ConversationMessageDeleteConfirmationUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = pluralStringResource(
                    id = R.plurals.delete_messages_confirmation_dialog_title,
                    count = deleteConfirmation.messageIds.size,
                    deleteConfirmation.messageIds.size,
                ),
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_message_confirmation_dialog_text),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(
                    text = stringResource(R.string.delete_message_confirmation_button),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                )
            }
        },
    )
}
