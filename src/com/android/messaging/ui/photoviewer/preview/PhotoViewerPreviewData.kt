package com.android.messaging.ui.photoviewer.preview

import androidx.core.net.toUri
import com.android.messaging.data.media.model.PhotoViewerItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun previewPhotoViewerItems(): ImmutableList<PhotoViewerItem> {
    return persistentListOf(
        previewPhotoViewerItem(index = 1),
        previewPhotoViewerItem(index = 2),
        previewPhotoViewerItem(index = 3),
    )
}

internal fun previewPhotoViewerItem(index: Int): PhotoViewerItem {
    return PhotoViewerItem(
        contentUri = "content://example/content/$index".toUri(),
        contentType = "image/jpeg",
        senderName = "Ada Lovelace",
        senderDestination = "+1555123000$index",
        receivedTimestampMillis = 1_735_689_600_000L,
        isDraft = false,
    )
}
