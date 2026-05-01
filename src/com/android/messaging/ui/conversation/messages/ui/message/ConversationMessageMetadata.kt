package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status

@Composable
internal fun ConversationMessageMetadata(
    message: ConversationMessageUiModel,
    metadataText: String?,
) {
    metadataText?.let {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            text = metadataText,
            style = MaterialTheme.typography.labelSmall,
            color = messageMetadataColor(message = message),
            textAlign = when {
                message.isIncoming -> TextAlign.Start
                else -> TextAlign.End
            },
        )
    }
}

@Composable
private fun messageMetadataColor(
    message: ConversationMessageUiModel,
): Color {
    return when (message.status) {
        Status.Outgoing.AwaitingRetry,
        Status.Outgoing.Failed,
        Status.Outgoing.FailedEmergencyNumber,
        Status.Incoming.DownloadFailed,
        Status.Incoming.ExpiredOrNotAvailable,
        -> MaterialTheme.colorScheme.error

        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
