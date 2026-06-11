package com.android.messaging.ui.conversation.mediapicker.component.capture

import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaCaptureShutterButtonTest :
    BaseConversationMediaCaptureComponentTest() {

    @Test
    fun photoIdle_forwardsCaptureClick() {
        setShutterButtonContent(
            captureMode = ConversationCaptureMode.Photo,
        )

        clickCaptureShutterButton()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onCaptureClick.invoke()
            }
        }
    }

    @Test
    fun photoCaptureInProgress_ignoresCaptureClick() {
        setShutterButtonContent(
            captureMode = ConversationCaptureMode.Photo,
            isPhotoCaptureInProgress = true,
        )

        clickCaptureShutterButton()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onCaptureClick.invoke()
            }
        }
    }

    @Test
    fun videoIdle_forwardsCaptureClick() {
        setShutterButtonContent(
            captureMode = ConversationCaptureMode.Video,
            isRecording = false,
        )

        clickCaptureShutterButton()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onCaptureClick.invoke()
            }
        }
    }

    @Test
    fun videoRecording_forwardsCaptureClick() {
        setShutterButtonContent(
            captureMode = ConversationCaptureMode.Video,
            isRecording = true,
        )

        clickCaptureShutterButton()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onCaptureClick.invoke()
            }
        }
    }
}
