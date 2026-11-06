package com.android.messaging.ui.conversation.mediapicker

import android.view.Display
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerCaptureSceneTest : BaseConversationMediaPickerTest() {

    @Test
    fun closeButton_photoMode_keepsPaddingFromPhotoPreview() {
        bindCamera()
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 448.dp),
            captureMode = ConversationCaptureMode.Photo,
        )

        assertCloseButtonPosition(
            top = 36.dp,
            left = 16.dp,
        )
    }

    @Test
    fun closeButton_videoMode_keepsPaddingFromVideoPreview() {
        bindCamera()
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 448.dp),
            captureMode = ConversationCaptureMode.Video,
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 40.dp,
        )
    }

    @Test
    fun closeButton_contentPadding_keepsPaddingFromShrunkPreview() {
        bindCamera()
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 448.dp),
            captureMode = ConversationCaptureMode.Photo,
            contentPadding = PaddingValues(bottom = 88.dp),
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 31.dp,
        )
    }

    @Test
    fun closeButton_cameraRatioDiffersFromNominal_followsCameraRatio() {
        bindCamera(videoAspectRatio = PHOTO_ASPECT_RATIO)
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 448.dp),
            captureMode = ConversationCaptureMode.Video,
        )

        assertCloseButtonPosition(
            top = 36.dp,
            left = 16.dp,
        )
    }

    @Test
    fun closeButton_photoModeBeforeCameraBinds_usesNominalPhotoRatio() {
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 448.dp),
            captureMode = ConversationCaptureMode.Photo,
        )

        assertCloseButtonPosition(
            top = 36.dp,
            left = 16.dp,
        )
    }

    @Test
    fun closeButton_videoModeBeforeCameraBinds_usesNominalVideoRatio() {
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 448.dp),
            captureMode = ConversationCaptureMode.Video,
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 40.dp,
        )
    }

    @Test
    @Config(qualifiers = "land")
    fun closeButton_landscapeBeforeCameraBinds_usesLandscapeNominalRatio() {
        setCaptureSceneContent(
            modifier = Modifier.size(width = 440.dp, height = 300.dp),
            captureMode = ConversationCaptureMode.Photo,
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 36.dp,
        )
    }

    @Test
    @Config(qualifiers = "land")
    fun closeButton_landscapeWindowOnPortraitDisplay_usesPortraitNominalRatio() {
        ShadowDisplayManager.changeDisplay(Display.DEFAULT_DISPLAY, "port")
        setCaptureSceneContent(
            modifier = Modifier.size(width = 425.dp, height = 300.dp),
            captureMode = ConversationCaptureMode.Photo,
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 116.dp,
        )
    }

    @Test
    fun closeButton_cameraPermissionDenied_keepsPaddingFromWholeArea() {
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 448.dp),
            captureMode = ConversationCaptureMode.Photo,
            cameraPermissionGranted = false,
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 16.dp,
        )
    }

    private fun bindCamera(
        photoAspectRatio: Float = PHOTO_ASPECT_RATIO,
        videoAspectRatio: Float = VIDEO_ASPECT_RATIO,
    ) {
        cameraState.photoAspectRatio.value = photoAspectRatio
        cameraState.videoAspectRatio.value = videoAspectRatio
    }

    private fun assertCloseButtonPosition(
        top: Dp,
        left: Dp,
    ) {
        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(
                    R.string.conversation_media_picker_close_content_description,
                ),
            )
            .assertTopPositionInRootIsEqualTo(expectedTop = top)
            .assertLeftPositionInRootIsEqualTo(expectedLeft = left)
    }

    private companion object {
        const val PHOTO_ASPECT_RATIO = 3f / 4f
        const val VIDEO_ASPECT_RATIO = 9f / 16f
    }
}
