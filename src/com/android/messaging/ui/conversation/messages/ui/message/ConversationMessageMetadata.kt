package com.android.messaging.ui.conversation.messages.ui.message

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.ui.buildConversationSimLinkAnnotatedString

private const val METADATA_SEPARATOR = " • "

@Composable
internal fun ConversationMessageMetadata(
    message: ConversationMessageUiModel,
    metadataText: String?,
    simDisplayName: String?,
    onSimSelectorClick: () -> Unit,
) {
    if (message.mmsDownload != null) {
        return
    }

    val linkColor = MaterialTheme.colorScheme.primary
    val resources = LocalResources.current

    val annotatedText = remember(
        metadataText,
        simDisplayName,
        linkColor,
        resources,
        onSimSelectorClick,
    ) {
        buildMessageMetadataAnnotatedString(
            metadataText = metadataText,
            simDisplayName = simDisplayName,
            simAnnotationTemplate = resources.getString(
                R.string.conversation_message_sim_annotation,
            ),
            linkColor = linkColor,
            onSimSelectorClick = onSimSelectorClick,
        )
    }

    if (annotatedText == null) {
        return
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        text = annotatedText,
        style = MaterialTheme.typography.labelSmall,
        color = messageMetadataColor(message = message),
        textAlign = when {
            message.isIncoming -> TextAlign.Start
            else -> TextAlign.End
        },
    )
}

private fun buildMessageMetadataAnnotatedString(
    metadataText: String?,
    simDisplayName: String?,
    simAnnotationTemplate: String,
    linkColor: Color,
    onSimSelectorClick: () -> Unit,
): AnnotatedString? {
    return when {
        metadataText == null && simDisplayName == null -> null

        simDisplayName == null -> {
            AnnotatedString(text = metadataText.orEmpty())
        }

        else -> {
            buildConversationSimLinkAnnotatedString(
                leadingText = metadataText,
                leadingSeparator = METADATA_SEPARATOR,
                simDisplayName = simDisplayName,
                annotationTemplate = simAnnotationTemplate,
                linkColor = linkColor,
                onSimSelectorClick = onSimSelectorClick,
            )
        }
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
        -> MaterialTheme.colorScheme.error

        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
