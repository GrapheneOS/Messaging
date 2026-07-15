package com.android.messaging.domain.photoviewer.usecase

import android.net.Uri
import javax.inject.Inject

internal interface NormalizePhotoViewerUri {
    operator fun invoke(uri: Uri): String
}

internal class NormalizePhotoViewerUriImpl @Inject constructor() : NormalizePhotoViewerUri {

    override fun invoke(uri: Uri): String {
        return uri
            .buildUpon()
            .clearQuery()
            .fragment(null)
            .build()
            .toString()
    }
}
