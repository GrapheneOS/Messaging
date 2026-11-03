package com.android.messaging.ui.conversation.links.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_OPEN_LINK_CONFIRM_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_OPEN_LINK_DISMISS_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.links.model.ConversationLinkConfirmationUiState
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationLinkConfirmationDialog(
    confirmation: ConversationLinkConfirmationUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.conversation_open_link_title))
        },
        text = {
            ConversationLinkConfirmationTarget(confirmation = confirmation)
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(tag = CONVERSATION_OPEN_LINK_CONFIRM_BUTTON_TEST_TAG),
                onClick = onConfirm,
            ) {
                Text(text = stringResource(R.string.conversation_open_link_confirm))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(tag = CONVERSATION_OPEN_LINK_DISMISS_BUTTON_TEST_TAG),
                onClick = onDismiss,
            ) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ConversationLinkConfirmationTarget(
    confirmation: ConversationLinkConfirmationUiState,
) {
    Column(
        modifier = Modifier.verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = confirmation.destination,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (confirmation.showsFullUri) {
            Text(
                text = confirmation.uri,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationLinkConfirmationDialogWebPreview() {
    MessagingPreviewTheme {
        ConversationLinkConfirmationDialog(
            confirmation = ConversationLinkConfirmationUiState(
                uri = "https://example.com/account/verify?session=4f2a9c",
                destination = "example.com",
                showsFullUri = true,
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationLinkConfirmationDialogPhonePreview() {
    MessagingPreviewTheme {
        ConversationLinkConfirmationDialog(
            confirmation = ConversationLinkConfirmationUiState(
                uri = "tel:%2B31%206%202222%203333",
                destination = "+31 6 2222 3333",
                showsFullUri = false,
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
