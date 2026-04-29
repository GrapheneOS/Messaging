package com.android.messaging.ui.conversation.v2.mediapicker

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.mediapicker.camera.ConversationCameraController
import com.android.messaging.ui.conversation.v2.mediapicker.component.review.ConversationMediaReviewScene
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

private enum class ConversationMediaPickerOverlayMode {
    Capture,
    Review,
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@Composable
internal fun ConversationMediaPickerScaffold(
    modifier: Modifier = Modifier,
    cameraController: ConversationCameraController,
    scaffoldState: BottomSheetScaffoldState,
    photoPickerSheetContent: @Composable () -> Unit,
    visualAttachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    conversationTitle: String?,
    captureMode: ConversationCaptureMode,
    reviewContentUri: String?,
    reviewRequestSequence: Int,
    isReviewVisible: Boolean,
    isSendActionEnabled: Boolean,
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    onClose: () -> Unit,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String>,
    onRequestAudioPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onSendClick: () -> Unit,
    onShowReview: (String) -> Unit,
    onClearReview: () -> Unit,
    onCaptureModeChange: (ConversationCaptureMode) -> Unit,
) {
    ConversationMediaPickerSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        photoPickerSheetContent = photoPickerSheetContent,
    ) { innerPadding ->
        ConversationMediaPickerOverlayHost(
            modifier = Modifier.fillMaxSize(),
            cameraController = cameraController,
            contentPadding = innerPadding,
            visualAttachments = visualAttachments,
            conversationTitle = conversationTitle,
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
            onCapturedMediaReady = onCapturedMediaReady,
            onSendClick = onSendClick,
            onShowReview = onShowReview,
            onClearReview = onClearReview,
            onCaptureModeChange = onCaptureModeChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationMediaPickerOverlayHost(
    modifier: Modifier = Modifier,
    cameraController: ConversationCameraController,
    contentPadding: PaddingValues,
    visualAttachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    conversationTitle: String?,
    captureMode: ConversationCaptureMode,
    reviewContentUri: String?,
    reviewRequestSequence: Int,
    isReviewVisible: Boolean,
    isSendActionEnabled: Boolean,
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    onClose: () -> Unit,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String>,
    onRequestAudioPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onSendClick: () -> Unit,
    onShowReview: (String) -> Unit,
    onClearReview: () -> Unit,
    onCaptureModeChange: (ConversationCaptureMode) -> Unit,
) {
    AnimatedContent(
        modifier = modifier
            .fillMaxSize(),
        targetState = resolveOverlayMode(isReviewVisible = isReviewVisible),
        transitionSpec = {
            pickerOverlayTransition()
        },
        label = "pickerOverlayMode",
    ) { currentOverlayMode ->
        when (currentOverlayMode) {
            ConversationMediaPickerOverlayMode.Capture -> {
                ConversationMediaPickerCaptureScene(
                    cameraController = cameraController,
                    contentPadding = contentPadding,
                    captureMode = captureMode,
                    cameraPermissionGranted = cameraPermissionGranted,
                    audioPermissionGranted = audioPermissionGranted,
                    onClose = onClose,
                    onRequestAudioPermission = onRequestAudioPermission,
                    onRequestCameraPermission = onRequestCameraPermission,
                    onCapturedMediaReady = onCapturedMediaReady,
                    onShowReview = onShowReview,
                    onCaptureModeChange = onCaptureModeChange,
                )
            }

            ConversationMediaPickerOverlayMode.Review -> {
                ConversationMediaReviewScene(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    attachments = visualAttachments,
                    conversationTitle = conversationTitle,
                    initiallyReviewedContentUri = reviewContentUri,
                    reviewRequestSequence = reviewRequestSequence,
                    isSendActionEnabled = isSendActionEnabled,
                    photoPickerSourceContentUriByAttachmentContentUri =
                        photoPickerSourceContentUriByAttachmentContentUri,
                    onAttachmentPreviewClick = onAttachmentPreviewClick,
                    onCaptionChange = onAttachmentCaptionChange,
                    onAttachmentRemove = onAttachmentRemove,
                    onAddMoreClick = onClearReview,
                    onClearReview = onClearReview,
                    onCloseClick = onClose,
                    onSendClick = {
                        onSendClick()
                        onClose()
                    },
                )
            }
        }
    }
}

private fun resolveOverlayMode(isReviewVisible: Boolean): ConversationMediaPickerOverlayMode {
    return when {
        isReviewVisible -> ConversationMediaPickerOverlayMode.Review
        else -> ConversationMediaPickerOverlayMode.Capture
    }
}

private fun pickerOverlayTransition(): ContentTransform {
    val enterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = 180,
            delayMillis = 40,
        ),
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )

    val exitTransition = fadeOut(
        animationSpec = tween(durationMillis = 100),
    ) + scaleOut(
        targetScale = 0.985f,
        animationSpec = tween(durationMillis = 100),
    )

    return enterTransition.togetherWith(exitTransition)
}
