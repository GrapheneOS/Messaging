package com.android.messaging.ui.common.components.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import kotlinx.collections.immutable.ImmutableList

private val AttachmentPreviewItemSpacing = 8.dp
private val AttachmentPreviewListHorizontalPadding = 12.dp
private val AttachmentPreviewListVerticalPadding = 4.dp

@Composable
internal fun <T> AttachmentPreviewRow(
    attachments: ImmutableList<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    if (attachments.isEmpty()) {
        return
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = AttachmentPreviewListHorizontalPadding,
            vertical = AttachmentPreviewListVerticalPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(space = AttachmentPreviewItemSpacing),
    ) {
        items(
            items = attachments,
            key = { item -> key(item) },
        ) { item ->
            itemContent(item)
        }
    }
}

@Composable
internal fun AttachmentPreviewRemoveButton(
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        modifier = modifier.size(size = size),
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = pluralStringResource(
                id = R.plurals.attachment_preview_close_content_description,
                count = 1,
            ),
        )
    }
}
