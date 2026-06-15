package com.android.messaging.ui.common.components.mediapreview

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf

@Stable
internal class MediaPreviewBitmapCache {
    private val cachedBackgroundBitmapsByContentUri = mutableStateMapOf<String, Bitmap>()

    operator fun get(contentUri: String): Bitmap? {
        return cachedBackgroundBitmapsByContentUri[contentUri]
    }

    fun put(contentUri: String, bitmap: Bitmap) {
        cachedBackgroundBitmapsByContentUri[contentUri] = bitmap
    }

    fun removeInactive(activeContentUris: Set<String>) {
        cachedBackgroundBitmapsByContentUri
            .keys
            .asSequence()
            .filterNot { it in activeContentUris }
            .toSet()
            .let { inactiveContentUris ->
                cachedBackgroundBitmapsByContentUri -= inactiveContentUris
            }
    }
}
