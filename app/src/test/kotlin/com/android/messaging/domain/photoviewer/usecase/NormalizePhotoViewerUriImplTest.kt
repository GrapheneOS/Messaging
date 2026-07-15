package com.android.messaging.domain.photoviewer.usecase

import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NormalizePhotoViewerUriImplTest {

    private val normalizePhotoViewerUri = NormalizePhotoViewerUriImpl()

    @Test
    fun invoke_whenUriHasQueryAndFragment_removesBoth() {
        val result = normalizePhotoViewerUri(
            uri = "content://example/images/1?version=2#preview".toUri(),
        )

        assertEquals("content://example/images/1", result)
    }

    @Test
    fun invoke_whenUriHasNoQueryOrFragment_returnsSameUriString() {
        val result = normalizePhotoViewerUri(
            uri = "content://example/images/1".toUri(),
        )

        assertEquals("content://example/images/1", result)
    }
}
