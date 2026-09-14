package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.messaging.testutil.performDisabledTouchClick
import com.android.messaging.testutil.performTouchClick
import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaCaptureControlsTest :
    BaseConversationMediaCaptureComponentTest() {

    @Test
    fun videoModeChip_forwardsVideoModeCallback() {
        setControlsContent(captureMode = ConversationCaptureMode.Photo)

        composeTestRule
            .onNodeWithText(videoModeLabel())
            .assertIsDisplayed()
            .performTouchClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onVideoModeClick.invoke()
            }
            verify(exactly = 0) {
                onPhotoModeClick.invoke()
            }
        }
    }

    @Test
    fun photoModeChip_forwardsPhotoModeCallback() {
        setControlsContent(captureMode = ConversationCaptureMode.Video)

        composeTestRule
            .onNodeWithText(photoModeLabel())
            .assertIsDisplayed()
            .performTouchClick()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onVideoModeClick.invoke()
            }
            verify(exactly = 1) {
                onPhotoModeClick.invoke()
            }
        }
    }

    @Test
    fun modeToggleAndSwitchCamera_disabledWhilePhotoCaptureInProgress() {
        setControlsContent(
            isPhotoCaptureInProgress = true,
        )

        composeTestRule
            .onNodeWithText(videoModeLabel())
            .performDisabledTouchClick()
        composeTestRule
            .onNodeWithContentDescription(switchCameraDescription())
            .assertIsNotEnabled()
            .performDisabledTouchClick()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onVideoModeClick.invoke()
            }
            verify(exactly = 0) {
                onSwitchCameraClick.invoke()
            }
        }
    }

    @Test
    fun recordingShowsTimerAndDisablesModeSwitchesAndSwitchCamera() {
        setControlsContent(
            captureMode = ConversationCaptureMode.Video,
            isRecording = true,
            recordingDurationMillis = RECORDING_DURATION_MILLIS,
        )

        composeTestRule
            .onNodeWithText(RECORDING_DURATION_TEXT)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(photoModeLabel())
            .performDisabledTouchClick()
        composeTestRule
            .onNodeWithContentDescription(switchCameraDescription())
            .assertIsNotEnabled()
            .performDisabledTouchClick()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onPhotoModeClick.invoke()
            }
            verify(exactly = 0) {
                onSwitchCameraClick.invoke()
            }
        }
    }

    @Test
    fun switchCamera_forwardsClickWhenIdle() {
        setControlsContent()

        composeTestRule
            .onNodeWithContentDescription(switchCameraDescription())
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onSwitchCameraClick.invoke()
            }
        }
    }

    private companion object {
        private const val RECORDING_DURATION_MILLIS = 65_000L
        private const val RECORDING_DURATION_TEXT = "01:05"
    }
}
