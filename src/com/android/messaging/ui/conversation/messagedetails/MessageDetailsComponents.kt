package com.android.messaging.ui.conversation.messagedetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessage
import com.android.messaging.ui.conversation.resolveDisplayName

private val MessageDetailsCardPadding = 16.dp

@Composable
internal fun MessageDetailsPreviewCard(
    preview: ConversationMessageUiModel,
    details: ConversationMessageDetails,
    modifier: Modifier = Modifier,
) {
    val simDisplayName = details.subscriptionLabel?.resolveDisplayName()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        ConversationMessage(
            modifier = Modifier.padding(all = 12.dp),
            message = preview,
            simDisplayName = simDisplayName,
            showIncomingParticipantIdentity = false,
        )
    }
}

@Composable
internal fun MessageDetailsStatusSection(
    sentTimestamp: Long?,
    receivedTimestamp: Long?,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageDetailsCard(
        title = stringResource(id = R.string.message_details_status_label),
        modifier = modifier,
    ) {
        receivedTimestamp?.let { timestamp ->
            MessageDetailsRow(
                label = stringResource(id = R.string.message_details_received_label),
                value = formatMessageDetailsTimestamp(timestampMillis = timestamp),
                onCopy = onCopy,
            )
        }

        sentTimestamp?.let { timestamp ->
            MessageDetailsRow(
                label = stringResource(id = R.string.message_details_sent_label),
                value = formatMessageDetailsTimestamp(timestampMillis = timestamp),
                onCopy = onCopy,
            )
        }
    }
}

@Composable
internal fun MessageDetailsMessageFields(
    details: ConversationMessageDetails,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageDetailsCard(
        title = stringResource(id = R.string.message_details_message_label),
        modifier = modifier,
    ) {
        MessageDetailsRow(
            label = stringResource(id = R.string.message_details_type_label),
            value = messageDetailsTypeText(details.type),
            onCopy = null,
        )

        details.sender?.let { sender ->
            MessageDetailsRow(
                label = stringResource(id = R.string.message_details_from_label),
                value = sender,
                onCopy = onCopy,
            )
        }

        details.recipients.takeIf { it.isNotEmpty() }?.let { recipients ->
            MessageDetailsRow(
                label = stringResource(id = R.string.message_details_to_label),
                value = recipients.joinToString(separator = ", "),
                onCopy = onCopy,
            )
        }
    }
}

@Composable
internal fun MessageDetailsDeliveryFields(
    details: ConversationMessageDetails,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val hasDeliveryFields = details.priority != null || details.sizeBytes != null

    if (!hasDeliveryFields) {
        return
    }

    MessageDetailsCard(
        title = stringResource(id = R.string.message_details_delivery_label),
        modifier = modifier,
    ) {
        details.priority?.let { priority ->
            MessageDetailsRow(
                label = stringResource(id = R.string.message_details_priority_label),
                value = messageDetailsPriorityText(priority),
                onCopy = null,
            )
        }

        details.sizeBytes?.let { sizeBytes ->
            MessageDetailsRow(
                label = stringResource(id = R.string.message_details_size_label),
                value = formatMessageDetailsSize(
                    context = context,
                    sizeBytes = sizeBytes,
                ),
                onCopy = onCopy,
            )
        }
    }
}

@Composable
internal fun MessageDetailsDebugSection(
    debug: ConversationMessageDetails.Debug,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(value = false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Surface(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.message_details_debug_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Icon(
                    imageVector = when {
                        isExpanded -> Icons.Rounded.KeyboardArrowUp
                        else -> Icons.Rounded.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            MessageDetailsCard {
                messageDetailsDebugEntries(debug = debug).forEach { entry ->
                    MessageDetailsDebugField(
                        label = entry.first,
                        value = entry.second,
                        onCopy = onCopy,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageDetailsCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        title?.let { sectionTitle ->
            Text(
                modifier = Modifier.padding(horizontal = MessageDetailsCardPadding),
                text = sectionTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun MessageDetailsRow(
    label: String,
    value: String,
    onCopy: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .copyOnLongPress(
                value = value,
                onCopy = onCopy,
            )
            .padding(MessageDetailsCardPadding),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            modifier = Modifier.weight(weight = 1f),
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun Modifier.copyOnLongPress(
    value: String,
    onCopy: ((String) -> Unit)?,
): Modifier {
    if (onCopy == null) {
        return this
    }

    val copyLabel = stringResource(R.string.copy_to_clipboard)
    return combinedClickable(
        onClick = {},
        onLongClickLabel = copyLabel,
        onLongClick = { onCopy(value) },
    )
}

@Composable
private fun MessageDetailsDebugField(
    label: String,
    value: String,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .copyOnLongPress(
                value = value,
                onCopy = onCopy,
            )
            .padding(MessageDetailsCardPadding),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun messageDetailsDebugEntries(
    debug: ConversationMessageDetails.Debug,
): List<Pair<String, String>> {
    return listOfNotNull(
        debug.messageId?.let { "Message id" to it },
        debug.telephonyUri?.let { "Telephony uri" to it },
        debug.conversationId?.let { "Conversation id" to it },
        debug.conversationTelephonyThreadId?.let { "Conversation thread id" to it.toString() },
        debug.telephonyThreadId?.let { "Telephony thread id" to it.toString() },
        debug.contentLocationUrl?.let { "Content location" to it },
        debug.threadRecipientIds?.let { "Thread recipient ids" to it },
        debug.threadRecipients?.let { "Thread recipients" to it },
        debug.sender?.let { "Sender" to it },
    )
}
