package com.android.messaging.ui.conversation.mediapicker

import androidx.activity.ComponentActivity
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationCameraController
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationPhotoFlashMode
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.util.ContentType
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule

internal abstract class BaseConversationMediaPickerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    protected lateinit var cameraController: ConversationCameraController
    protected lateinit var cameraState: CameraControllerState

    protected val onClose = mockk<() -> Unit>(relaxed = true)
    protected val onAttachmentStartRequest = mockk<() -> Boolean>()
    protected val onCapturedMediaReady = mockk<(ConversationCapturedMedia) -> Unit>(relaxed = true)
    protected val onShowReview = mockk<(String) -> Unit>(relaxed = true)
    protected val onCaptureModeChange = mockk<(ConversationCaptureMode) -> Unit>(relaxed = true)
    protected val onAttachmentPreviewClick =
        mockk<(ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit>(relaxed = true)
    protected val onAttachmentCaptionChange = mockk<(String, String) -> Unit>(relaxed = true)
    protected val onAttachmentRemove = mockk<(String) -> Unit>(relaxed = true)
    protected val onRequestAudioPermission = mockk<() -> Unit>(relaxed = true)
    protected val onRequestCameraPermission = mockk<() -> Unit>(relaxed = true)
    protected val onSendClick = mockk<() -> Unit>(relaxed = true)
    protected val onClearReview = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUpBaseConversationMediaPickerTest() {
        clearAllMocks()
        cameraState = CameraControllerState()
        cameraController = mockk(relaxed = true)
        every { cameraController.hasFlashUnit } returns cameraState.hasFlashUnit
        every { cameraController.isPhotoCaptureInProgress } returns
            cameraState.isPhotoCaptureInProgress
        every { cameraController.isRecording } returns cameraState.isRecording
        every { cameraController.photoFlashMode } returns cameraState.photoFlashMode
        every { cameraController.recordingDurationMillis } returns
            cameraState.recordingDurationMillis
        every { cameraController.surfaceRequest } returns cameraState.surfaceRequest
        every { onAttachmentStartRequest.invoke() } returns true
    }

    protected fun setCaptureRouteContent(
        audioPermissionGranted: Boolean = true,
        captureMode: ConversationCaptureMode = ConversationCaptureMode.Photo,
        cameraPermissionGranted: Boolean = true,
    ) {
        setThemedContent {
            ConversationMediaCaptureRoute(
                modifier = Modifier.fillMaxSize(),
                cameraController = cameraController,
                audioPermissionGranted = audioPermissionGranted,
                captureMode = captureMode,
                cameraPermissionGranted = cameraPermissionGranted,
                onClose = onClose,
                onRequestAudioPermission = onRequestAudioPermission,
                onAttachmentStartRequest = onAttachmentStartRequest,
                onShowReview = onShowReview,
                onCapturedMediaReady = onCapturedMediaReady,
                onCaptureModeChange = onCaptureModeChange,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    protected fun setScaffoldContent(
        visualAttachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia> =
            persistentListOf(imageAttachment()),
        captureMode: ConversationCaptureMode = ConversationCaptureMode.Photo,
        reviewContentUri: String? = IMAGE_CONTENT_URI,
        reviewRequestSequence: Int = 0,
        isReviewVisible: Boolean,
        isSendActionEnabled: Boolean = true,
        cameraPermissionGranted: Boolean = false,
        audioPermissionGranted: Boolean = true,
        photoPickerSourceContentUriByAttachmentContentUri:
        ImmutableMap<String, String> = persistentMapOf(),
    ) {
        setThemedContent {
            val sheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
            )
            val scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = sheetState,
            )

            ConversationMediaPickerScaffold(
                modifier = Modifier.fillMaxSize(),
                cameraController = cameraController,
                scaffoldState = scaffoldState,
                photoPickerSheetContent = {
                    Box(
                        modifier = Modifier.size(1.dp),
                    )
                },
                visualAttachments = visualAttachments,
                conversationTitle = CONVERSATION_TITLE,
                captureMode = captureMode,
                reviewContentUri = reviewContentUri,
                reviewRequestSequence = reviewRequestSequence,
                isReviewVisible = isReviewVisible,
                isSendActionEnabled = isSendActionEnabled,
                cameraPermissionGranted = cameraPermissionGranted,
                audioPermissionGranted = audioPermissionGranted,
                onClose = onClose,
                onAttachmentPreviewClick = onAttachmentPreviewClick,
                onAttachmentCaptionChange = onAttachmentCaptionChange,
                onAttachmentRemove = onAttachmentRemove,
                photoPickerSourceContentUriByAttachmentContentUri =
                photoPickerSourceContentUriByAttachmentContentUri,
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

    protected fun clickCaptureControl() {
        composeTestRule
            .onNodeWithTag(CONVERSATION_MEDIA_CAPTURE_SHUTTER_BUTTON_TEST_TAG)
            .performClick()
    }

    protected fun imageAttachment(
        contentUri: String = IMAGE_CONTENT_URI,
        captionText: String = IMAGE_CAPTION,
    ): ComposerAttachmentUiModel.Resolved.VisualMedia.Image {
        return ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
            key = IMAGE_KEY,
            contentType = ContentType.IMAGE_JPEG,
            contentUri = contentUri,
            captionText = captionText,
            width = IMAGE_WIDTH,
            height = IMAGE_HEIGHT,
        )
    }

    protected fun capturedPhoto(): ConversationCapturedMedia {
        return ConversationCapturedMedia(
            contentUri = CAPTURED_PHOTO_URI,
            contentType = ContentType.IMAGE_JPEG,
        )
    }

    protected fun capturedVideo(): ConversationCapturedMedia {
        return ConversationCapturedMedia(
            contentUri = CAPTURED_VIDEO_URI,
            contentType = ContentType.VIDEO_MP4,
        )
    }

    private fun setThemedContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            AppTheme(content = content)
        }
    }

    protected class CameraControllerState(
        val hasFlashUnit: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val isPhotoCaptureInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val isRecording: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val photoFlashMode: MutableStateFlow<ConversationPhotoFlashMode> =
            MutableStateFlow(ConversationPhotoFlashMode.Off),
        val recordingDurationMillis: MutableStateFlow<Long> = MutableStateFlow(0L),
        val surfaceRequest: MutableStateFlow<SurfaceRequest?> =
            MutableStateFlow(null),
    )

    protected companion object {
        const val CONVERSATION_TITLE = "Weekend plan"
        const val IMAGE_KEY = "image-1"
        const val IMAGE_CONTENT_URI = "content://media/picker/image/1"
        const val IMAGE_CAPTION = "Ready to send"
        const val CAPTURED_PHOTO_URI = "content://media/picker/captured/photo"
        const val CAPTURED_VIDEO_URI = "content://media/picker/captured/video"
        const val IMAGE_WIDTH = 640
        const val IMAGE_HEIGHT = 480
    }
}
