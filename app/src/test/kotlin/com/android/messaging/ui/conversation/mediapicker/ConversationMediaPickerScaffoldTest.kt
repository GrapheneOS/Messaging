package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerScaffoldTest : BaseConversationMediaPickerTest() {

    @Test
    fun captureModeRendersCameraPermissionFallbackAndForwardsPermissionAction() {
        setScaffoldContent(
            isReviewVisible = false,
            cameraPermissionGranted = false,
        )

        composeTestRule
            .onNodeWithText(string(R.string.conversation_media_picker_camera_permission_message))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.conversation_media_picker_allow_camera))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onRequestCameraPermission.invoke()
            }
            verify(exactly = 0) {
                onClearReview.invoke()
            }
        }
    }

    @Test
    fun reviewModeRendersAttachmentReviewInsteadOfCaptureFallback() {
        setScaffoldContent(
            isReviewVisible = true,
            cameraPermissionGranted = false,
        )

        composeTestRule
            .onNodeWithText(IMAGE_CAPTION)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(R.string.conversation_media_picker_camera_permission_message))
            .assertDoesNotExist()
    }

    @Test
    fun reviewModeSend_forwardsSendAndClosesPicker() {
        setScaffoldContent(
            isReviewVisible = true,
        )

        composeTestRule
            .onNodeWithContentDescription(string(R.string.sendButtonContentDescription))
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onSendClick.invoke()
            }
            verify(exactly = 1) {
                onClose.invoke()
            }
        }
    }

    @Test
    fun reviewModeAddMore_clearsReview() {
        setScaffoldContent(
            isReviewVisible = true,
        )

        composeTestRule
            .onNodeWithContentDescription(
                string(R.string.conversation_media_picker_add_more_content_description),
            )
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onClearReview.invoke()
            }
            verify(exactly = 0) {
                onClose.invoke()
            }
        }
    }

    private fun string(resourceId: Int): String {
        return targetContext.getString(resourceId)
    }
}
