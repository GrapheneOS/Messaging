package com.android.messaging.ui.photoviewer.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
internal data class PhotoViewerLaunchRequest(
    val initialPhotoUri: String,
    val photosUri: String,
    val sourceBounds: PhotoViewerSourceBounds,
    val initialPartId: String? = null,
)

@Immutable
internal data class PhotoViewerLaunchRequestKey(
    val initialPhotoUri: String,
    val photosUri: String,
    val initialPartId: String?,
)

internal fun photoViewerLaunchRequestKey(
    launchRequest: PhotoViewerLaunchRequest,
): PhotoViewerLaunchRequestKey {
    return PhotoViewerLaunchRequestKey(
        initialPhotoUri = launchRequest.initialPhotoUri,
        photosUri = launchRequest.photosUri,
        initialPartId = launchRequest.initialPartId,
    )
}
