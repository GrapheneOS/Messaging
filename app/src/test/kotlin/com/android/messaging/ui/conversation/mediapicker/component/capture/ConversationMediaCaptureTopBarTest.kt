package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.android.messaging.testutil.performDisabledTouchClick
import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationPhotoFlashMode
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaCaptureTopBarTest :
    BaseConversationMediaCaptureComponentTest() {

    @Test
    fun closeButton_forwardsClick() {
        setTopBarContent()

        composeTestRule
            .onNodeWithContentDescription(closeDescription())
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onCloseClick.invoke()
            }
        }
    }

    @Test
    fun flashHiddenForVideoMode() {
        setTopBarContent(
            captureMode = ConversationCaptureMode.Video,
            hasFlashUnit = true,
        )

        composeTestRule
            .onNodeWithContentDescription(flashDescription())
            .assertDoesNotExist()
    }

    @Test
    fun flashHiddenWhenFlashUnitUnavailable() {
        setTopBarContent(
            captureMode = ConversationCaptureMode.Photo,
            hasFlashUnit = false,
        )

        composeTestRule
            .onNodeWithContentDescription(flashDescription())
            .assertDoesNotExist()
    }

    @Test
    fun flashButton_forwardsClickWhenPhotoModeIdle() {
        setTopBarContent(
            captureMode = ConversationCaptureMode.Photo,
            hasFlashUnit = true,
            photoFlashMode = ConversationPhotoFlashMode.Auto,
        )

        composeTestRule
            .onNodeWithContentDescription(flashDescription())
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onFlashClick.invoke()
            }
        }
    }

    @Test
    fun flashButton_disabledWhilePhotoCapturing() {
        setTopBarContent(
            captureMode = ConversationCaptureMode.Photo,
            hasFlashUnit = true,
            isPhotoCaptureInProgress = true,
            photoFlashMode = ConversationPhotoFlashMode.On,
        )

        composeTestRule
            .onNodeWithContentDescription(flashDescription())
            .assertIsNotEnabled()
            .performDisabledTouchClick()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onFlashClick.invoke()
            }
        }
    }

    @Test
    fun flashButton_disabledWhileRecording() {
        setTopBarContent(
            captureMode = ConversationCaptureMode.Photo,
            hasFlashUnit = true,
            isRecording = true,
        )

        composeTestRule
            .onNodeWithContentDescription(flashDescription())
            .assertIsNotEnabled()
            .performDisabledTouchClick()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onFlashClick.invoke()
            }
        }
    }
}
