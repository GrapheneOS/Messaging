package com.android.messaging.ui.common.components.mediapreview

import androidx.compose.runtime.Immutable

@Immutable
internal data class MediaPreviewItem(
    val contentUri: String,
    val contentType: String,
    val isVideo: Boolean,
)
