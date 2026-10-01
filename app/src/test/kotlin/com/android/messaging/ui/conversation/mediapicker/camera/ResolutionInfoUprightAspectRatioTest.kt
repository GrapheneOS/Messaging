package com.android.messaging.ui.conversation.mediapicker.camera

import android.graphics.Rect
import android.util.Size
import androidx.camera.core.ResolutionInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ResolutionInfoUprightAspectRatioTest {

    @Test
    fun sidewaysSensorBuffer_isSwappedToPortrait() {
        assertEquals(PORTRAIT_RATIO, landscapeCrop(rotationDegrees = 90).toUprightAspectRatio())
        assertEquals(PORTRAIT_RATIO, landscapeCrop(rotationDegrees = 270).toUprightAspectRatio())
    }

    @Test
    fun alreadyUprightBuffer_keepsItsShape() {
        assertEquals(LANDSCAPE_RATIO, landscapeCrop(rotationDegrees = 0).toUprightAspectRatio())
        assertEquals(LANDSCAPE_RATIO, landscapeCrop(rotationDegrees = 180).toUprightAspectRatio())
    }

    private fun landscapeCrop(rotationDegrees: Int): ResolutionInfo {
        return ResolutionInfo(
            Size(4080, 3072),
            Rect(0, 0, 4080, 3072),
            rotationDegrees,
        )
    }

    private companion object {
        private const val LANDSCAPE_RATIO = 4080f / 3072f
        private const val PORTRAIT_RATIO = 3072f / 4080f
    }
}
