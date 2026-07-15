package com.android.messaging.ui.photoviewer.component

import androidx.compose.runtime.Immutable

@Immutable
internal data class PhotoViewerContentKey(
    val page: Int,
    val contentUri: String,
)
