package com.android.messaging.ui.conversation.mediapicker

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
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
import androidx.photopicker.compose.EmbeddedPhotoPickerState
import androidx.photopicker.compose.ExperimentalPhotoPickerComposeApi
import androidx.photopicker.compose.rememberEmbeddedPhotoPickerState
import com.android.messaging.data.media.model.ConversationCapturedMedia
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.mediapicker.camera.BindConversationCameraLifecycleEffect
import com.android.messaging.ui.conversation.mediapicker.camera.ConversationCameraController
import com.android.messaging.ui.conversation.mediapicker.camera.rememberConversationCameraController
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "ConversationMediaPicker"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPhotoPickerComposeApi::class)
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
    val visualAttachments = rememberVisualMediaAttachments(attachments = attachments)
    val isReviewVisible = state.isReviewRequested && visualAttachments.isNotEmpty()
    val lifecycleOwner = LocalLifecycleOwner.current

    BindConversationCameraLifecycleEffect(
        cameraController = cameraController,
        cameraPermissionGranted = cameraPermissionGranted,
        isCameraPreviewVisible = !isReviewVisible,
        lifecycleOwner = lifecycleOwner,
    )

    ConversationMediaPickerContent(
        modifier = modifier,
        cameraController = cameraController,
        visualAttachments = visualAttachments,
        isReviewVisible = isReviewVisible,
        state = state,
        conversationTitle = conversationTitle,
        isSendActionEnabled = isSendActionEnabled,
        cameraPermissionGranted = cameraPermissionGranted,
        audioPermissionGranted = audioPermissionGranted,
        onClose = onClose,
        onAttachmentPreviewClick = onAttachmentPreviewClick,
        onAttachmentCaptionChange = onAttachmentCaptionChange,
        onAttachmentRemove = onAttachmentRemove,
        photoPickerSourceContentUriByAttachmentContentUri =
        photoPickerSourceContentUriByAttachmentContentUri,
        onPhotoPickerMediaSelected = onPhotoPickerMediaSelected,
        onPhotoPickerMediaDeselected = onPhotoPickerMediaDeselected,
        onRequestAudioPermission = onRequestAudioPermission,
        onRequestCameraPermission = onRequestCameraPermission,
        onCapturedMediaReady = onCapturedMediaReady,
        onSendClick = onSendClick,
    )
}

@Composable
private fun rememberVisualMediaAttachments(
    attachments: ImmutableList<ComposerAttachmentUiModel>,
): ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia> {
    return remember(attachments) {
        attachments
            .asSequence()
            .filterIsInstance<ComposerAttachmentUiModel.Resolved.VisualMedia>()
            .toImmutableList()
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@Composable
private fun rememberConversationEmbeddedPhotoPickerFeatureInfo(): EmbeddedPhotoPickerFeatureInfo {
    return remember {
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun rememberConversationEmbeddedPhotoPickerState(
    sheetState: SheetState,
    state: ConversationMediaPickerState,
    coroutineScope: CoroutineScope,
    onPhotoPickerMediaSelected: (List<String>) -> Unit,
    onPhotoPickerMediaDeselected: (List<String>) -> Unit,
): EmbeddedPhotoPickerState {
    return rememberEmbeddedPhotoPickerState(
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun SyncEmbeddedPhotoPickerExpansionEffect(
    sheetState: SheetState,
    embeddedPhotoPickerState: EmbeddedPhotoPickerState,
) {
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
}

@OptIn(ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun rememberPickerBackedAttachmentRemoveCallback(
    coroutineScope: CoroutineScope,
    embeddedPhotoPickerState: EmbeddedPhotoPickerState,
    photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String>,
    onAttachmentRemove: (String) -> Unit,
): (String) -> Unit {
    return remember(
        coroutineScope,
        embeddedPhotoPickerState,
        photoPickerSourceContentUriByAttachmentContentUri,
        onAttachmentRemove,
    ) {
        { contentUri ->
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
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun ConversationMediaPickerContent(
    modifier: Modifier,
    cameraController: ConversationCameraController,
    visualAttachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    isReviewVisible: Boolean,
    state: ConversationMediaPickerState,
    conversationTitle: String?,
    isSendActionEnabled: Boolean,
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
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
    )
    val embeddedPhotoPickerState = rememberConversationEmbeddedPhotoPickerState(
        sheetState = sheetState,
        state = state,
        coroutineScope = coroutineScope,
        onPhotoPickerMediaSelected = onPhotoPickerMediaSelected,
        onPhotoPickerMediaDeselected = onPhotoPickerMediaDeselected,
    )
    SyncEmbeddedPhotoPickerExpansionEffect(
        sheetState = sheetState,
        embeddedPhotoPickerState = embeddedPhotoPickerState,
    )

    ConversationMediaPickerScaffoldContent(
        modifier = modifier,
        cameraController = cameraController,
        scaffoldState = scaffoldState,
        embeddedPhotoPickerState = embeddedPhotoPickerState,
        embeddedPhotoPickerFeatureInfo = rememberConversationEmbeddedPhotoPickerFeatureInfo(),
        visualAttachments = visualAttachments,
        isReviewVisible = isReviewVisible,
        state = state,
        conversationTitle = conversationTitle,
        isSendActionEnabled = isSendActionEnabled,
        cameraPermissionGranted = cameraPermissionGranted,
        audioPermissionGranted = audioPermissionGranted,
        onClose = onClose,
        onAttachmentPreviewClick = onAttachmentPreviewClick,
        onAttachmentCaptionChange = onAttachmentCaptionChange,
        onAttachmentRemove = rememberPickerBackedAttachmentRemoveCallback(
            coroutineScope = coroutineScope,
            embeddedPhotoPickerState = embeddedPhotoPickerState,
            photoPickerSourceContentUriByAttachmentContentUri =
            photoPickerSourceContentUriByAttachmentContentUri,
            onAttachmentRemove = onAttachmentRemove,
        ),
        photoPickerSourceContentUriByAttachmentContentUri =
        photoPickerSourceContentUriByAttachmentContentUri,
        onRequestAudioPermission = onRequestAudioPermission,
        onRequestCameraPermission = onRequestCameraPermission,
        onCapturedMediaReady = onCapturedMediaReady,
        onSendClick = onSendClick,
    )
}

@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun ConversationMediaPickerScaffoldContent(
    modifier: Modifier,
    cameraController: ConversationCameraController,
    scaffoldState: BottomSheetScaffoldState,
    embeddedPhotoPickerState: EmbeddedPhotoPickerState,
    embeddedPhotoPickerFeatureInfo: EmbeddedPhotoPickerFeatureInfo,
    visualAttachments: ImmutableList<ComposerAttachmentUiModel.Resolved.VisualMedia>,
    isReviewVisible: Boolean,
    state: ConversationMediaPickerState,
    conversationTitle: String?,
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
) {
    ConversationMediaPickerScaffold(
        modifier = modifier,
        cameraController = cameraController,
        scaffoldState = scaffoldState,
        photoPickerSheetContent = {
            ConversationEmbeddedPhotoPickerContent(
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
        onAttachmentRemove = onAttachmentRemove,
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

@SuppressLint("NewApi")
@OptIn(ExperimentalPhotoPickerComposeApi::class)
@Composable
private fun ConversationEmbeddedPhotoPickerContent(
    state: EmbeddedPhotoPickerState,
    embeddedPhotoPickerFeatureInfo: EmbeddedPhotoPickerFeatureInfo,
) {
    EmbeddedPhotoPicker(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        state = state,
        embeddedPhotoPickerFeatureInfo = embeddedPhotoPickerFeatureInfo,
    )
}
