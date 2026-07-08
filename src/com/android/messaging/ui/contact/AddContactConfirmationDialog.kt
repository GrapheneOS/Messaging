package com.android.messaging.ui.contact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.participant.ParticipantAvatar
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.contact.model.AddContactUiState
import com.android.messaging.ui.core.MessagingPreviewTheme

private val DialogPadding = 24.dp
private val AvatarSize = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddContactConfirmationDialog(
    uiState: AddContactUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(DialogPadding),
            ) {
                Text(
                    text = stringResource(R.string.add_contact_confirmation_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                AddContactDestination(uiState = uiState)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(text = stringResource(R.string.add_contact_confirmation))
                }

                Spacer(modifier = Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun AddContactDestination(uiState: AddContactUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParticipantAvatar(
            avatarUri = uiState.avatarUri,
            size = AvatarSize,
            fallbackLabel = participantAvatarLabel(uiState.destination),
            colorSeedCode = participantColorSeed(uiState.destination),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = uiState.destination,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics {
                contentDescription = uiState.vocalizedDestination
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun AddContactConfirmationDialogPhonePreview() {
    MessagingPreviewTheme {
        AddContactConfirmationDialog(
            uiState = AddContactUiState(
                avatarUri = null,
                destination = "+1 555-0134",
                vocalizedDestination = "+1 555-0134",
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
