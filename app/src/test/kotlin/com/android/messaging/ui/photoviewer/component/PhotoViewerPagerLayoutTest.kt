package com.android.messaging.ui.photoviewer.component

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

internal class PhotoViewerPagerLayoutTest {

    @Test
    fun resolvePhotoViewerPagerLayout_carousel_showsNeighborPages() {
        val layout = resolvePhotoViewerPagerLayout(maxWidth = 400.dp, immersiveProgress = 0f)

        assertPagerLayout(
            pageWidth = 360f,
            pageSpacing = 12f,
            horizontalInset = 20f,
            layout = layout,
        )
    }

    @Test
    fun resolvePhotoViewerPagerLayout_halfwayToImmersive_interpolatesEveryDimension() {
        val layout = resolvePhotoViewerPagerLayout(maxWidth = 400.dp, immersiveProgress = 0.5f)

        assertPagerLayout(
            pageWidth = 380f,
            pageSpacing = 6f,
            horizontalInset = 10f,
            layout = layout,
        )
    }

    @Test
    fun resolvePhotoViewerPagerLayout_immersive_fillsWidth() {
        val layout = resolvePhotoViewerPagerLayout(maxWidth = 400.dp, immersiveProgress = 1f)

        assertPagerLayout(
            pageWidth = 400f,
            pageSpacing = 0f,
            horizontalInset = 0f,
            layout = layout,
        )
    }

    private fun assertPagerLayout(
        pageWidth: Float,
        pageSpacing: Float,
        horizontalInset: Float,
        layout: PhotoViewerPagerLayout,
    ) {
        assertEquals(pageWidth, layout.pageWidth.value, DP_DELTA)
        assertEquals(pageSpacing, layout.pageSpacing.value, DP_DELTA)
        assertEquals(horizontalInset, layout.horizontalInset.value, DP_DELTA)
    }

    private companion object {
        private const val DP_DELTA = 0.001f
    }
}
