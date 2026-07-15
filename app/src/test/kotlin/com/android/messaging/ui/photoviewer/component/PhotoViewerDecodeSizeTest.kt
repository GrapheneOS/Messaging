package com.android.messaging.ui.photoviewer.component

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

internal class PhotoViewerDecodeSizeTest {

    @Test
    fun resolvePhotoViewerDecodeSize_scalesToOneAndHalfViewportSize() {
        val decodeSize = resolvePhotoViewerDecodeSize(
            displayedImageSize = IntSize(width = 1080, height = 1920),
        )

        assertEquals(IntSize(width = 1620, height = 2880), decodeSize)
    }

    @Test
    fun resolvePhotoViewerDecodeSize_capsDimensionsIndependently() {
        val decodeSize = resolvePhotoViewerDecodeSize(
            displayedImageSize = IntSize(width = 3000, height = 2500),
        )

        assertEquals(IntSize(width = 4096, height = 3750), decodeSize)
    }

    @Test
    fun resolvePhotoViewerDecodeSize_coercesInvalidDimensionsToOne() {
        val decodeSize = resolvePhotoViewerDecodeSize(
            displayedImageSize = IntSize(width = 0, height = -100),
        )

        assertEquals(IntSize(width = 1, height = 1), decodeSize)
    }

    @Test
    fun resolvePhotoViewerDecodeSize_preservesLandscapeBoundsBeforeCap() {
        val decodeSize = resolvePhotoViewerDecodeSize(
            displayedImageSize = IntSize(width = 1600, height = 900),
        )

        assertEquals(IntSize(width = 2400, height = 1350), decodeSize)
    }

    @Test
    fun resolvePhotoViewerDecodeSize_preservesPortraitBoundsBeforeCap() {
        val decodeSize = resolvePhotoViewerDecodeSize(
            displayedImageSize = IntSize(width = 900, height = 1600),
        )

        assertEquals(IntSize(width = 1350, height = 2400), decodeSize)
    }

    @Test
    fun resolvePhotoViewerDecodeSize_roundsHalfPixelsUp() {
        val decodeSize = resolvePhotoViewerDecodeSize(
            displayedImageSize = IntSize(width = 901, height = 601),
        )

        assertEquals(IntSize(width = 1352, height = 902), decodeSize)
    }
}
