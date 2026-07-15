package com.android.messaging.ui.photoviewer.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.media.model.PhotoViewerItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class PhotoViewerUiState(
    val loadState: PhotoViewerLoadState = PhotoViewerLoadState.Loading,
    val items: ImmutableList<PhotoViewerItem> = persistentListOf(),
    val currentPage: Int = 0,
    val displayMode: PhotoViewerDisplayMode = PhotoViewerDisplayMode.Carousel,
    val isMetadataSheetVisible: Boolean = false,
    val isClosing: Boolean = false,
)

internal enum class PhotoViewerLoadState {
    Loading,
    Loaded,
    Empty,
    Error,
}

internal enum class PhotoViewerDisplayMode {
    Carousel,
    Immersive,
}
