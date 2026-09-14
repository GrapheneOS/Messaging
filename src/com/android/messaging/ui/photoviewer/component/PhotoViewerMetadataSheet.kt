package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.common.components.bottomBarInsets
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_METADATA_RECEIVED_TIMESTAMP_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_METADATA_SENDER_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_METADATA_SHEET_TEST_TAG
import com.android.messaging.ui.photoviewer.preview.previewPhotoViewerItems
import com.android.messaging.util.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoViewerMetadataSheet(
    isVisible: Boolean,
    item: PhotoViewerItem?,
    actionsEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    if (!isVisible || item == null) {
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = Modifier.testTag(tag = PHOTO_VIEWER_METADATA_SHEET_TEST_TAG),
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            PhotoViewerMetadataSheetDragHandle()
        },
    ) {
        PhotoViewerMetadataSheetContent(
            item = item,
            actionsEnabled = actionsEnabled,
            onSaveClick = onSaveClick,
            onShareClick = onShareClick,
        )
    }
}

@Composable
private fun PhotoViewerMetadataSheetDragHandle() {
    Surface(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .width(width = 36.dp)
            .height(height = 4.dp),
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
    ) {
        Spacer(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PhotoViewerMetadataSheetContent(
    item: PhotoViewerItem,
    actionsEnabled: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(bottomBarInsets())
            .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        PhotoViewerMetadataGroup(item = item)
        PhotoViewerMetadataActions(
            actionsEnabled = actionsEnabled,
            onSaveClick = onSaveClick,
            onShareClick = onShareClick,
        )
    }
}

@Composable
private fun PhotoViewerMetadataGroup(
    item: PhotoViewerItem,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        PhotoViewerMetadataRow(
            label = stringResource(id = R.string.message_details_from_label),
            value = photoViewerParticipantTitle(item = item),
            valueModifier = Modifier.testTag(tag = PHOTO_VIEWER_METADATA_SENDER_TEST_TAG),
        )
        if (!item.isDraft) {
            PhotoViewerMetadataRow(
                modifier = Modifier.testTag(
                    tag = PHOTO_VIEWER_METADATA_RECEIVED_TIMESTAMP_TEST_TAG,
                ),
                label = stringResource(id = R.string.message_details_received_label),
                value = Dates.getMessageTimeString(item.receivedTimestampMillis).toString(),
            )
        }
        PhotoViewerMetadataRow(
            label = stringResource(id = R.string.message_details_type_label),
            value = item.contentType,
        )
    }
}

@Composable
private fun PhotoViewerMetadataActions(
    actionsEnabled: Boolean,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        FilledTonalButton(
            modifier = Modifier
                .weight(weight = 1f)
                .height(height = 56.dp),
            enabled = actionsEnabled,
            shape = CircleShape,
            onClick = onShareClick,
        ) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(text = stringResource(id = R.string.action_share))
        }

        Button(
            modifier = Modifier
                .weight(weight = 1f)
                .height(height = 56.dp),
            enabled = actionsEnabled,
            shape = CircleShape,
            onClick = onSaveClick,
        ) {
            Icon(
                imageVector = Icons.Rounded.FileDownload,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(text = stringResource(id = R.string.save))
        }
    }
}

@Composable
private fun PhotoViewerMetadataRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = valueModifier,
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewLightDark
@Composable
private fun PhotoViewerMetadataSheetContentPreview() {
    MessagingPreviewTheme {
        Surface {
            PhotoViewerMetadataSheetContent(
                item = previewPhotoViewerItems().first(),
                actionsEnabled = true,
                onSaveClick = {},
                onShareClick = {},
            )
        }
    }
}
