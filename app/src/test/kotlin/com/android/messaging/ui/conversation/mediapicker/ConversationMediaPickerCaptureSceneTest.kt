package com.android.messaging.ui.conversation.mediapicker

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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerCaptureSceneTest : BaseConversationMediaPickerTest() {

    @Before
    fun setUpAspectRatios() {
        cameraState.photoAspectRatio.value = PHOTO_ASPECT_RATIO
        cameraState.videoAspectRatio.value = VIDEO_ASPECT_RATIO
    }

    @Test
    fun photoMode_anchorsCloseButtonToPhotoPreview() {
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 400.dp),
            captureMode = ConversationCaptureMode.Photo,
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 16.dp,
        )
    }

    @Test
    fun videoMode_anchorsCloseButtonToVideoPreview() {
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 400.dp),
            captureMode = ConversationCaptureMode.Video,
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 53.5.dp,
        )
    }

    @Test
    fun contentPadding_shrinksPreviewTheCloseButtonIsAnchoredTo() {
        setCaptureSceneContent(
            modifier = Modifier.size(width = 300.dp, height = 400.dp),
            captureMode = ConversationCaptureMode.Photo,
            contentPadding = PaddingValues(bottom = 100.dp),
        )

        assertCloseButtonPosition(
            top = 12.dp,
            left = 53.5.dp,
        )
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
