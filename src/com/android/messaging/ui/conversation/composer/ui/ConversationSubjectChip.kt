package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SUBJECT_CHIP_TEST_TAG
import com.android.messaging.ui.core.MessagingPreviewColumn

private val SUBJECT_CHIP_TEXT_VERTICAL_PADDING = 12.dp
private const val PREVIEW_LONG_SUBJECT_TEXT =
    "Upcoming weekend plans, dinner reservation details, parking instructions, and confirmation number"

@Composable
internal fun ConversationSubjectChip(
    subjectText: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag = CONVERSATION_SUBJECT_CHIP_TEST_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(
                    start = 16.dp,
                    top = SUBJECT_CHIP_TEXT_VERTICAL_PADDING,
                    bottom = SUBJECT_CHIP_TEXT_VERTICAL_PADDING,
                ),
            text = subjectText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            modifier = Modifier
                .padding(end = 4.dp)
                .testTag(tag = CONVERSATION_SUBJECT_CHIP_CLEAR_BUTTON_TEST_TAG),
            onClick = onClear,
        ) {
            Icon(
                modifier = Modifier.size(size = 20.dp),
                imageVector = Icons.Rounded.Cancel,
                contentDescription = stringResource(R.string.delete_subject_content_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationSubjectChipPreview() {
    MessagingPreviewColumn {
        ConversationSubjectChip(
            subjectText = "Dinner reservation details",
            onClick = {},
            onClear = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationSubjectChipLongTextPreview() {
    MessagingPreviewColumn {
        ConversationSubjectChip(
            subjectText = PREVIEW_LONG_SUBJECT_TEXT,
            onClick = {},
            onClear = {},
        )
    }
}
