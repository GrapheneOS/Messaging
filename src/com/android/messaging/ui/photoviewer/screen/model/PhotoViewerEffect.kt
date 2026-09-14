package com.android.messaging.ui.photoviewer.screen.model

import android.net.Uri

internal sealed interface PhotoViewerEffect {
    data object Finish : PhotoViewerEffect

    data class Save(
        val uri: Uri,
        val contentType: String,
    ) : PhotoViewerEffect

    data class Share(
        val uri: Uri,
        val contentType: String,
    ) : PhotoViewerEffect

    data class Forward(
        val uri: Uri,
        val contentType: String,
    ) : PhotoViewerEffect
}
