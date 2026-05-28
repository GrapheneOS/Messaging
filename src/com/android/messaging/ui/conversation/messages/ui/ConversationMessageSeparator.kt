package com.android.messaging.ui.conversation.messages.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn

private val messagesSeparatorSpacing = 12.dp
private val messagesSeparatorPadding = PaddingValues(
    horizontal = 14.dp,
    vertical = 6.dp,
)

@Composable
internal fun ColumnWithSeparator(
    modifier: Modifier,
    showDateSeparator: Boolean,
    dateSeparatorText: String?,
    content: @Composable () -> Unit,
) {
    val verticalSpace = when {
        showDateSeparator -> messagesSeparatorSpacing
        else -> 0.dp
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = verticalSpace),
    ) {
        if (showDateSeparator && dateSeparatorText != null) {
            ConversationDateSeparator(
                text = dateSeparatorText,
            )
        }

        content()
    }
}

@Composable
private fun ConversationDateSeparator(
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(messagesSeparatorPadding),
        )
    }
}

@PreviewLightDark
@Composable
private fun ColumnWithSeparatorStatePreview() {
    ConversationMessageSeparatorPreviewColumn {
        ColumnWithSeparatorPreviewItem(
            showDateSeparator = true,
            dateSeparatorText = "Today",
            contentText = "Visible separator",
        )
        ColumnWithSeparatorPreviewItem(
            showDateSeparator = false,
            dateSeparatorText = null,
            contentText = "No separator",
        )
        ColumnWithSeparatorPreviewItem(
            showDateSeparator = false,
            dateSeparatorText = "Yesterday",
            contentText = "Date text ignored",
        )
        ColumnWithSeparatorPreviewItem(
            showDateSeparator = true,
            dateSeparatorText = null,
            contentText = "Separator requested without text",
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationDateSeparatorTextPreview() {
    ConversationMessageSeparatorPreviewColumn {
        ConversationDateSeparator(
            text = "Today",
        )
        ConversationDateSeparator(
            text = "Mon, May 25",
        )
        ConversationDateSeparator(
            text = "Wednesday, September 30, 2026",
        )
        ConversationDateSeparator(
            text = "Wednesday, September 30, 2026 at 11:59 PM",
        )
    }
}

@PreviewLightDark
@Composable
private fun ColumnWithSeparatorContentPreview() {
    ConversationMessageSeparatorPreviewColumn {
        ColumnWithSeparator(
            modifier = Modifier,
            showDateSeparator = true,
            dateSeparatorText = "Today",
        ) {
            ConversationMessageSeparatorPreviewContent(
                text = "First message after a day boundary",
            )
        }
        ColumnWithSeparator(
            modifier = Modifier,
            showDateSeparator = false,
            dateSeparatorText = null,
        ) {
            ConversationMessageSeparatorPreviewContent(
                text = "Second message in the same separated group",
            )
        }
        ColumnWithSeparator(
            modifier = Modifier,
            showDateSeparator = false,
            dateSeparatorText = null,
        ) {
            ConversationMessageSeparatorPreviewContent(
                text = "Clustered continuation without extra separator spacing",
            )
        }
        ColumnWithSeparator(
            modifier = Modifier,
            showDateSeparator = false,
            dateSeparatorText = null,
        ) {
            ConversationMessageSeparatorPreviewContent(
                text = "Another continuation row",
            )
        }
    }
}

@Composable
private fun ConversationMessageSeparatorPreviewColumn(content: @Composable () -> Unit) {
    MessagingPreviewColumn {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ColumnWithSeparatorPreviewItem(
    showDateSeparator: Boolean,
    dateSeparatorText: String?,
    contentText: String,
) {
    ColumnWithSeparator(
        modifier = Modifier,
        showDateSeparator = showDateSeparator,
        dateSeparatorText = dateSeparatorText,
    ) {
        ConversationMessageSeparatorPreviewContent(
            text = contentText,
        )
    }
}

@Composable
private fun ConversationMessageSeparatorPreviewContent(
    text: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            modifier = Modifier.padding(all = 12.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
