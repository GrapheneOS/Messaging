package com.android.messaging.ui.common.components.composer

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme

private val SubjectChipTextVerticalPadding = 12.dp

@Composable
internal fun MessageSubjectChip(
    subjectText: String,
    onClick: (() -> Unit)?,
    onClear: () -> Unit,
    clearButtonTestTag: String,
    modifier: Modifier = Modifier,
) {
    val clickableModifier = when (onClick) {
        null -> Modifier
        else -> Modifier.clickable(onClick = onClick)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(
                    start = 16.dp,
                    top = SubjectChipTextVerticalPadding,
                    bottom = SubjectChipTextVerticalPadding,
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
                .testTag(tag = clearButtonTestTag),
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

@Preview
@Composable
private fun MessageSubjectChipPreview() {
    AppTheme {
        MessageSubjectChip(
            subjectText = "Trip photos",
            onClick = {},
            onClear = {},
            clearButtonTestTag = "",
        )
    }
}
