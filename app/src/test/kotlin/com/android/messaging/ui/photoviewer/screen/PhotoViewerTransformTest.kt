package com.android.messaging.ui.photoviewer.screen

import androidx.compose.ui.unit.IntSize
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import org.junit.Assert.assertEquals
import org.junit.Test

private const val FLOAT_DELTA = 0.001f

internal class PhotoViewerTransformTest {

    @Test
    fun resolvePhotoViewerTransform_whenRootSizeIsZero_usesFallbackAlphaAndScale() {
        val transform = resolvePhotoViewerTransform(
            sourceBounds = PhotoViewerSourceBounds(),
            rootSize = IntSize.Zero,
            progress = 1f,
        )

        assertPhotoViewerTransform(
            expected = PhotoViewerTransform(
                scale = 0.95f,
                translationX = 0f,
                translationY = 0f,
                alpha = 0f,
            ),
            actual = transform,
        )
    }

    @Test
    fun resolvePhotoViewerTransform_whenSourceBoundsAreInvalid_usesFallbackStartTransform() {
        val transform = resolvePhotoViewerTransform(
            sourceBounds = PhotoViewerSourceBounds(),
            rootSize = IntSize(width = 1000, height = 800),
            progress = 0f,
        )

        assertPhotoViewerTransform(
            expected = PhotoViewerTransform(
                scale = 0.95f,
                translationX = 0f,
                translationY = 24f,
                alpha = 0f,
            ),
            actual = transform,
        )
    }

    @Test
    fun resolvePhotoViewerTransform_whenSourceBoundsAreInvalid_interpolatesFallbackToIdentity() {
        val transform = resolvePhotoViewerTransform(
            sourceBounds = PhotoViewerSourceBounds(),
            rootSize = IntSize(width = 1000, height = 800),
            progress = 0.5f,
        )

        assertPhotoViewerTransform(
            expected = PhotoViewerTransform(
                scale = 0.975f,
                translationX = 0f,
                translationY = 12f,
                alpha = 0.5f,
            ),
            actual = transform,
        )
    }

    @Test
    fun resolvePhotoViewerTransform_whenSourceBoundsAreValid_usesSourceCenterAndScale() {
        val transform = resolvePhotoViewerTransform(
            sourceBounds = PhotoViewerSourceBounds(
                left = 100,
                top = 200,
                right = 300,
                bottom = 500,
            ),
            rootSize = IntSize(width = 1000, height = 800),
            progress = 0f,
        )

        assertPhotoViewerTransform(
            expected = PhotoViewerTransform(
                scale = 0.375f,
                translationX = -300f,
                translationY = -50f,
                alpha = 0f,
            ),
            actual = transform,
        )
    }

    @Test
    fun resolvePhotoViewerTransform_whenSourceBoundsAreValid_interpolatesToIdentity() {
        val transform = resolvePhotoViewerTransform(
            sourceBounds = PhotoViewerSourceBounds(
                left = 100,
                top = 200,
                right = 300,
                bottom = 500,
            ),
            rootSize = IntSize(width = 1000, height = 800),
            progress = 0.5f,
        )

        assertPhotoViewerTransform(
            expected = PhotoViewerTransform(
                scale = 0.6875f,
                translationX = -150f,
                translationY = -25f,
                alpha = 0.5f,
            ),
            actual = transform,
        )
    }

    @Test
    fun resolvePhotoViewerTransform_whenProgressIsComplete_returnsIdentityTransform() {
        val transform = resolvePhotoViewerTransform(
            sourceBounds = PhotoViewerSourceBounds(
                left = 100,
                top = 200,
                right = 300,
                bottom = 500,
            ),
            rootSize = IntSize(width = 1000, height = 800),
            progress = 1f,
        )

        assertPhotoViewerTransform(
            expected = PhotoViewerTransform(),
            actual = transform,
        )
    }

    @Test
    fun resolvePhotoViewerTransform_clampsTinySourceBoundsToMinimumScale() {
        val transform = resolvePhotoViewerTransform(
            sourceBounds = PhotoViewerSourceBounds(
                left = 495,
                top = 395,
                right = 505,
                bottom = 405,
            ),
            rootSize = IntSize(width = 1000, height = 800),
            progress = 0f,
        )

        assertPhotoViewerTransform(
            expected = PhotoViewerTransform(
                scale = 0.1f,
                translationX = 0f,
                translationY = 0f,
                alpha = 0f,
            ),
            actual = transform,
        )
    }

    private fun assertPhotoViewerTransform(
        expected: PhotoViewerTransform,
        actual: PhotoViewerTransform,
    ) {
        assertEquals(expected.scale, actual.scale, FLOAT_DELTA)
        assertEquals(expected.translationX, actual.translationX, FLOAT_DELTA)
        assertEquals(expected.translationY, actual.translationY, FLOAT_DELTA)
        assertEquals(expected.alpha, actual.alpha, FLOAT_DELTA)
    }
}
