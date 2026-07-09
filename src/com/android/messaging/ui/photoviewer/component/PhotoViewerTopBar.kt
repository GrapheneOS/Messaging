package com.android.messaging.ui.photoviewer.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContentColor
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_CLOSE_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_DETAILS_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_FORWARD_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_SAVE_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_SHARE_BUTTON_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_TIMESTAMP_TEST_TAG
import com.android.messaging.ui.photoviewer.PHOTO_VIEWER_TITLE_TEST_TAG
import com.android.messaging.util.Dates

@Composable
internal fun PhotoViewerTopBar(
    isVisible: Boolean,
    item: PhotoViewerItem?,
    actionsEnabled: Boolean,
    navigationBarInsets: WindowInsets = WindowInsets.navigationBars,
    onMetadataClick: () -> Unit,
    onCloseClick: () -> Unit,
    onForwardClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .displayCutoutPadding(),
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(alignment = Alignment.TopCenter),
        ) {
            PhotoViewerTopBarContent(
                item = item,
                actionsEnabled = actionsEnabled,
                navigationBarInsets = navigationBarInsets,
                onMetadataClick = onMetadataClick,
                onCloseClick = onCloseClick,
                onForwardClick = onForwardClick,
                onSaveClick = onSaveClick,
                onShareClick = onShareClick,
            )
        }
    }
}

@Composable
private fun PhotoViewerTopBarContent(
    item: PhotoViewerItem?,
    actionsEnabled: Boolean,
    navigationBarInsets: WindowInsets,
    onMetadataClick: () -> Unit,
    onCloseClick: () -> Unit,
    onForwardClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val contentColor = pickerOverlayContentColor()
    val secondaryContentColor = contentColor.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .windowInsetsPadding(
                insets = navigationBarInsets.only(sides = WindowInsetsSides.Horizontal),
            )
            .padding(all = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        PhotoViewerTopIconButton(
            modifier = Modifier.testTag(tag = PHOTO_VIEWER_CLOSE_BUTTON_TEST_TAG),
            contentDescription = stringResource(id = R.string.action_close),
            imageVector = Icons.Rounded.Close,
            tint = contentColor,
            onClick = onCloseClick,
        )

        PhotoViewerTitleBlock(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(start = 8.dp, end = 4.dp),
            item = item,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
        )

        PhotoViewerTopIconButton(
            modifier = Modifier.testTag(tag = PHOTO_VIEWER_SAVE_BUTTON_TEST_TAG),
            contentDescription = stringResource(id = R.string.save),
            enabled = actionsEnabled,
            imageVector = Icons.Rounded.FileDownload,
            tint = contentColor,
            onClick = onSaveClick,
        )
        PhotoViewerTopIconButton(
            modifier = Modifier.testTag(tag = PHOTO_VIEWER_SHARE_BUTTON_TEST_TAG),
            contentDescription = stringResource(id = R.string.action_share),
            enabled = actionsEnabled,
            imageVector = Icons.Rounded.Share,
            tint = contentColor,
            onClick = onShareClick,
        )
        PhotoViewerOverflowMenu(
            actionsEnabled = actionsEnabled,
            contentColor = contentColor,
            onForwardClick = onForwardClick,
            onMetadataClick = onMetadataClick,
        )
    }
}

@Composable
private fun PhotoViewerTitleBlock(
    modifier: Modifier,
    item: PhotoViewerItem?,
    contentColor: Color,
    secondaryContentColor: Color,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.testTag(tag = PHOTO_VIEWER_TITLE_TEST_TAG),
            text = photoViewerParticipantTitle(item = item),
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item?.isDraft != true) {
            Text(
                modifier = Modifier.testTag(tag = PHOTO_VIEWER_TIMESTAMP_TEST_TAG),
                text = photoViewerTimestamp(item = item),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PhotoViewerTopIconButton(
    modifier: Modifier = Modifier,
    contentDescription: String,
    imageVector: ImageVector,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = when {
                enabled -> tint
                else -> tint.copy(alpha = 0.4f)
            },
        )
    }
}

@Composable
private fun PhotoViewerOverflowMenu(
    actionsEnabled: Boolean,
    contentColor: Color,
    onForwardClick: () -> Unit,
    onMetadataClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(value = false) }

    Box {
        PhotoViewerTopIconButton(
            modifier = Modifier
                .testTag(tag = PHOTO_VIEWER_OVERFLOW_BUTTON_TEST_TAG),
            contentDescription = stringResource(id = R.string.more_options),
            imageVector = Icons.Rounded.MoreVert,
            tint = contentColor,
            onClick = {
                isExpanded = true
            },
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
            },
        ) {
            DropdownMenuItem(
                modifier = Modifier
                    .testTag(tag = PHOTO_VIEWER_FORWARD_MENU_ITEM_TEST_TAG),
                text = {
                    Text(text = stringResource(id = R.string.message_context_menu_forward_message))
                },
                enabled = actionsEnabled,
                onClick = {
                    isExpanded = false
                    onForwardClick()
                },
            )
            DropdownMenuItem(
                modifier = Modifier.testTag(tag = PHOTO_VIEWER_DETAILS_MENU_ITEM_TEST_TAG),
                text = {
                    Text(text = stringResource(id = R.string.photo_viewer_details_title))
                },
                onClick = {
                    isExpanded = false
                    onMetadataClick()
                },
            )
        }
    }
}

private fun photoViewerTimestamp(item: PhotoViewerItem?): String {
    return item
        ?.let { Dates.getMessageTimeString(it.receivedTimestampMillis).toString() }
        .orEmpty()
}
