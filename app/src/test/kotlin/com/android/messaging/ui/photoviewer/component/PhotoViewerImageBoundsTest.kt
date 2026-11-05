package com.android.messaging.ui.photoviewer.component

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

internal class PhotoViewerImageBoundsTest {

    @Test
    fun resolvePhotoViewerImageBounds_portraitWithCarouselPadding_fitsBetweenPaddings() {
        val bounds = resolvePhotoViewerImageBounds(
            containerSize = IntSize(width = 1000, height = 2000),
            imageAspectRatio = 0.5f,
            topPadding = 200,
            bottomPadding = 200,
        )

        assertEquals(IntRect(left = 100, top = 200, right = 900, bottom = 1800), bounds)
    }

    @Test
    fun resolvePhotoViewerImageBounds_portraitHalfwayToImmersive_growsBetweenEndStates() {
        val bounds = resolvePhotoViewerImageBounds(
            containerSize = IntSize(width = 1000, height = 2000),
            imageAspectRatio = 0.5f,
            topPadding = 100,
            bottomPadding = 100,
        )

        assertEquals(IntRect(left = 50, top = 100, right = 950, bottom = 1900), bounds)
    }

    @Test
    fun resolvePhotoViewerImageBounds_portraitWithoutPadding_fillsContainerHeight() {
        val bounds = resolvePhotoViewerImageBounds(
            containerSize = IntSize(width = 1000, height = 2000),
            imageAspectRatio = 0.25f,
            topPadding = 0,
            bottomPadding = 0,
        )

        assertEquals(IntRect(left = 250, top = 0, right = 750, bottom = 2000), bounds)
    }

    @Test
    fun resolvePhotoViewerImageBounds_landscape_fillsWidthAndCentersInPaddedArea() {
        val bounds = resolvePhotoViewerImageBounds(
            containerSize = IntSize(width = 1000, height = 2000),
            imageAspectRatio = 2f,
            topPadding = 300,
            bottomPadding = 100,
        )

        assertEquals(IntRect(left = 0, top = 850, right = 1000, bottom = 1350), bounds)
    }

    @Test
    fun resolvePhotoViewerImageBounds_paddingTallerThanContainer_collapsesToEmptyBounds() {
        val bounds = resolvePhotoViewerImageBounds(
            containerSize = IntSize(width = 100, height = 100),
            imageAspectRatio = 0.75f,
            topPadding = 80,
            bottomPadding = 80,
        )

        assertEquals(IntSize.Zero, bounds.size)
    }
}
