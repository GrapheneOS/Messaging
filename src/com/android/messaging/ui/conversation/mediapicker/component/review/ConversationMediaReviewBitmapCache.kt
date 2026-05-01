package com.android.messaging.ui.conversation.mediapicker.component.review

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf

@Stable
internal class ConversationMediaReviewBitmapCache {
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
