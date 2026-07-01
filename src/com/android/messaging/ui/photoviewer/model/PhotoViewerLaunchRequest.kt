package com.android.messaging.ui.photoviewer.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class PhotoViewerLaunchRequest(
    val initialPhotoUri: String,
    val photosUri: String,
    val sourceBounds: PhotoViewerSourceBounds,
)

@Immutable
internal data class PhotoViewerLaunchRequestKey(
    val initialPhotoUri: String,
    val photosUri: String,
)

internal fun photoViewerLaunchRequestKey(
    launchRequest: PhotoViewerLaunchRequest,
): PhotoViewerLaunchRequestKey {
    return PhotoViewerLaunchRequestKey(
        initialPhotoUri = launchRequest.initialPhotoUri,
        photosUri = launchRequest.photosUri,
    )
}
