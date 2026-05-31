package com.android.messaging.ui.conversation.mediapicker.component.review

import android.graphics.Bitmap
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

internal class ConversationMediaReviewBitmapCacheTest {

    @Test
    fun putAndGet_returnsBitmapByContentUri() {
        val cache = ConversationMediaReviewBitmapCache()
        val bitmap = mockk<Bitmap>()

        cache.put(
            contentUri = CONTENT_URI,
            bitmap = bitmap,
        )

        assertSame(bitmap, cache[CONTENT_URI])
    }

    @Test
    fun removeInactive_removesOnlyInactiveContentUris() {
        val cache = ConversationMediaReviewBitmapCache()
        val activeBitmap = mockk<Bitmap>()
        val inactiveBitmap = mockk<Bitmap>()
        cache.put(
            contentUri = CONTENT_URI,
            bitmap = activeBitmap,
        )
        cache.put(
            contentUri = INACTIVE_CONTENT_URI,
            bitmap = inactiveBitmap,
        )

        cache.removeInactive(activeContentUris = setOf(CONTENT_URI))

        assertSame(activeBitmap, cache[CONTENT_URI])
        assertNull(cache[INACTIVE_CONTENT_URI])
    }

    private companion object {
        private const val CONTENT_URI = "content://media/image/1"
        private const val INACTIVE_CONTENT_URI = "content://media/image/2"
    }
}
