package com.android.messaging.ui.conversation.v2.mediapicker

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.photopicker.compose.EmbeddedPhotoPicker
import androidx.photopicker.compose.ExperimentalPhotoPickerComposeApi
import androidx.photopicker.compose.rememberEmbeddedPhotoPickerState
import com.android.messaging.ui.conversation.v2.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.v2.mediapicker.camera.BindConversationCameraLifecycleEffect
import com.android.messaging.ui.conversation.v2.mediapicker.camera.rememberConversationCameraController
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "ConversationMediaPicker"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPhotoPickerComposeApi::class)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@Composable
internal fun ConversationMediaPicker(
    modifier: Modifier = Modifier,
    attachments: ImmutableList<ComposerAttachmentUiModel>,
    conversationTitle: String?,
    isSendActionEnabled: Boolean,
    state: ConversationMediaPickerState,
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean,
    onClose: () -> Unit,
    onAttachmentPreviewClick: (ComposerAttachmentUiModel.Resolved.VisualMedia) -> Unit,
    onAttachmentCaptionChange: (String, String) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String>,
    onPhotoPickerMediaSelected: (List<String>) -> Unit,
    onPhotoPickerMediaDeselected: (List<String>) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCapturedMediaReady: (ConversationCapturedMedia) -> Unit,
    onSendClick: () -> Unit,
) {
    val cameraController = rememberConversationCameraController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val visualAttachments = remember(attachments) {
        attachments
            .asSequence()
            .filterIsInstance<ComposerAttachmentUiModel.Resolved.VisualMedia>()
            .toImmutableList()
    }

    val isReviewVisible = state.isReviewRequested && visualAttachments.isNotEmpty()
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
    )

    val embeddedPhotoPickerFeatureInfo = remember {
        EmbeddedPhotoPickerFeatureInfo.Builder()
            .setMaxSelectionLimit(MediaStore.getPickImagesMaxLimit())
            .setMimeTypes(
                listOf(
                    ContentType.IMAGE_UNSPECIFIED,
                    ContentType.VIDEO_UNSPECIFIED,
                ),
            )
            .setOrderedSelection(true)
            .build()
    }

    val embeddedPhotoPickerState = rememberEmbeddedPhotoPickerState(
        initialExpandedValue = false,
        onSessionError = {
            LogUtil.w(TAG, "Embedded photo picker session failed", it)
        },
        onUriPermissionGranted = { uris ->
            val contentUris = uris.map(Uri::toString)
            onPhotoPickerMediaSelected(contentUris)
            contentUris.lastOrNull()?.let(state::showReview)
        },
        onUriPermissionRevoked = { uris ->
            onPhotoPickerMediaDeselected(uris.map(Uri::toString))
        },
        onSelectionComplete = {
            coroutineScope.launch(Dispatchers.Main.immediate) {
                sheetState.partialExpand()
            }
        },
    )

    LaunchedEffect(sheetState, embeddedPhotoPickerState) {
        snapshotFlow {
            sheetState.currentValue == SheetValue.Expanded ||
                sheetState.targetValue == SheetValue.Expanded
        }
            .distinctUntilChanged()
            .collect { isExpanded ->
                embeddedPhotoPickerState.setCurrentExpanded(expanded = isExpanded)
            }
    }

    val onPickerBackedAttachmentRemove = { contentUri: String ->
        val sourceContentUri = photoPickerSourceContentUriByAttachmentContentUri[contentUri]
            ?: contentUri
        coroutineScope.launch(Dispatchers.Main.immediate) {
            try {
                embeddedPhotoPickerState.deselectUri(uri = sourceContentUri.toUri())
            } catch (e: IllegalStateException) {
                LogUtil.w(TAG, "Unable to deselect photo picker URI $sourceContentUri", e)
            }
        }
        onAttachmentRemove(contentUri)
    }

    BindConversationCameraLifecycleEffect(
        cameraController = cameraController,
        cameraPermissionGranted = cameraPermissionGranted,
        isCameraPreviewVisible = !isReviewVisible,
        lifecycleOwner = lifecycleOwner,
    )

    ConversationMediaPickerScaffold(
        modifier = modifier,
        cameraController = cameraController,
        scaffoldState = scaffoldState,
        photoPickerSheetContent = {
            EmbeddedPhotoPicker(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .fillMaxSize(),
                state = embeddedPhotoPickerState,
                embeddedPhotoPickerFeatureInfo = embeddedPhotoPickerFeatureInfo,
            )
        },
        visualAttachments = visualAttachments,
        conversationTitle = conversationTitle,
        captureMode = state.captureMode,
        reviewContentUri = state.reviewContentUri,
        reviewRequestSequence = state.reviewRequestSequence,
        isReviewVisible = isReviewVisible,
        isSendActionEnabled = isSendActionEnabled,
        cameraPermissionGranted = cameraPermissionGranted,
        audioPermissionGranted = audioPermissionGranted,
        onClose = onClose,
        onAttachmentPreviewClick = onAttachmentPreviewClick,
        onAttachmentCaptionChange = onAttachmentCaptionChange,
        onAttachmentRemove = onPickerBackedAttachmentRemove,
        photoPickerSourceContentUriByAttachmentContentUri =
            photoPickerSourceContentUriByAttachmentContentUri,
        onRequestAudioPermission = onRequestAudioPermission,
        onRequestCameraPermission = onRequestCameraPermission,
        onCapturedMediaReady = onCapturedMediaReady,
        onSendClick = onSendClick,
        onShowReview = state::showReview,
        onClearReview = state::clearReview,
        onCaptureModeChange = state::updateCaptureMode,
    )
}
