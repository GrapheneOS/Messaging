package com.android.messaging.ui.photoviewer.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class PhotoViewerLaunchRequest(
    val initialPhotoUri: String,
    val photosUri: String,
    val sourceBounds: PhotoViewerSourceBounds,
    val initialPhotoOccurrenceIndex: Int = 0,
)

@Immutable
internal data class PhotoViewerLaunchRequestKey(
    val initialPhotoUri: String,
    val photosUri: String,
    val initialPhotoOccurrenceIndex: Int,
)

internal fun photoViewerLaunchRequestKey(
    launchRequest: PhotoViewerLaunchRequest,
): PhotoViewerLaunchRequestKey {
    return PhotoViewerLaunchRequestKey(
        initialPhotoUri = launchRequest.initialPhotoUri,
        photosUri = launchRequest.photosUri,
        initialPhotoOccurrenceIndex = launchRequest.initialPhotoOccurrenceIndex,
    )
}
