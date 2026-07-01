package com.android.messaging.data.media.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PhotoViewerItems(
    val items: ImmutableList<PhotoViewerItem>,
    val initialIndex: Int,
)

internal sealed interface PhotoViewerItemsLoadResult {
    data class Loaded(
        val photoViewerItems: PhotoViewerItems,
    ) : PhotoViewerItemsLoadResult

    data object Error : PhotoViewerItemsLoadResult
}
