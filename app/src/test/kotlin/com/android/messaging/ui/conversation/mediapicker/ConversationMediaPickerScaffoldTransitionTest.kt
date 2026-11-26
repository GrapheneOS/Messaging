package com.android.messaging.ui.conversation.mediapicker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerScaffoldTransitionTest : BaseConversationMediaPickerTest() {

    @Test
    fun removingTheLastReviewedAttachment_keepsTheConversationCovered() {
        var visualAttachments by mutableStateOf(
            persistentListOf<ComposerAttachmentUiModel.Resolved.VisualMedia>(imageAttachment()),
        )
        setScaffoldOverConversation(visualAttachments = { visualAttachments })
        composeTestRule.mainClock.autoAdvance = false

        visualAttachments = persistentListOf()
        val conversationFrames = (0 until TRANSITION_FRAMES).filter {
            composeTestRule.mainClock.advanceTimeByFrame()
            conversationShowsThrough()
        }

        assertTrue(
            "frames showing the conversation: $conversationFrames",
            conversationFrames.isEmpty(),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setScaffoldOverConversation(
        visualAttachments: () -> ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    ) {
        composeTestRule.setContent {
            AppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = CONVERSATION_COLOR),
                ) {
                    ConversationMediaPickerScaffold(
                        modifier = Modifier.fillMaxSize(),
                        cameraController = cameraController,
                        scaffoldState = rememberBottomSheetScaffoldState(),
                        photoPickerContent = {},
                        visualAttachments = visualAttachments(),
                        conversationTitle = CONVERSATION_TITLE,
                        captureMode = ConversationCaptureMode.Photo,
                        reviewContentUri = IMAGE_CONTENT_URI,
                        reviewRequestSequence = 0,
                        isReviewVisible = visualAttachments().isNotEmpty(),
                        isSendActionEnabled = true,
                        cameraPermissionGranted = false,
                        audioPermissionGranted = true,
                        onClose = onClose,
                        onAttachmentPreviewClick = onAttachmentPreviewClick,
                        onAttachmentCaptionChange = onAttachmentCaptionChange,
                        onAttachmentRemove = onAttachmentRemove,
                        photoPickerSourceContentUriByAttachmentContentUri = persistentMapOf(),
                        onRequestAudioPermission = onRequestAudioPermission,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onAttachmentStartRequest = onAttachmentStartRequest,
                        onCapturedMediaReady = onCapturedMediaReady,
                        onSendClick = onSendClick,
                        onShowReview = onShowReview,
                        onClearReview = onClearReview,
                        onCaptureModeChange = onCaptureModeChange,
                    )
                }
            }
        }
    }

    // The stand-in photo picker is transparent, so only the area above the sheet is checked.
    private fun conversationShowsThrough(): Boolean {
        val view = composeTestRule.activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        return (0 until bitmap.height / 3).any { y ->
            (0 until bitmap.width).any { x ->
                val pixel = bitmap.getPixel(x, y)
                AndroidColor.red(pixel) > CONVERSATION_MINIMUM_RED_BLUE &&
                    AndroidColor.blue(pixel) > CONVERSATION_MINIMUM_RED_BLUE &&
                    AndroidColor.green(pixel) < CONVERSATION_MAXIMUM_GREEN
            }
        }
    }

    private companion object {
        val CONVERSATION_COLOR = Color.Magenta
        const val CONVERSATION_MINIMUM_RED_BLUE = 120
        const val CONVERSATION_MAXIMUM_GREEN = 60

        // Longer than the overlay transition.
        const val TRANSITION_FRAMES = 20
    }
}
