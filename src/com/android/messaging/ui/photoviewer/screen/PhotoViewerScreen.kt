package com.android.messaging.ui.photoviewer.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.photoviewer.component.PhotoViewerMetadataSheet
import com.android.messaging.ui.photoviewer.component.rememberPhotoViewerDismissDragState
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import com.android.messaging.ui.photoviewer.model.photoViewerLaunchRequestKey
import com.android.messaging.ui.photoviewer.preview.previewPhotoViewerItems
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerLoadState
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerUiState

@Composable
internal fun PhotoViewerScreen(
    modifier: Modifier = Modifier,
    launchRequest: PhotoViewerLaunchRequest,
    onFinish: () -> Unit,
    screenModel: PhotoViewerScreenModel = hiltViewModel<PhotoViewerViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(launchRequest) {
        screenModel.onLaunchRequest(launchRequest = launchRequest)
    }

    PhotoViewerScreenEffects(
        screenModel = screenModel,
        onFinish = onFinish,
    )
    PhotoViewerSystemBarsEffect(displayMode = uiState.displayMode)

    BackHandler(enabled = uiState.isMetadataSheetVisible) {
        screenModel.onMetadataDismissed()
    }

    BackHandler(enabled = !uiState.isMetadataSheetVisible) {
        screenModel.onCloseClick()
    }

    PhotoViewerScreenContent(
        modifier = modifier,
        launchRequest = launchRequest,
        uiState = uiState,
        onPageSettled = screenModel::onPageSettled,
        onToggleDisplayMode = screenModel::onToggleDisplayMode,
        onEnterImmersiveMode = screenModel::onEnterImmersiveMode,
        onMetadataClick = screenModel::onMetadataClick,
        onMetadataDismissed = screenModel::onMetadataDismissed,
        onCloseClick = screenModel::onCloseClick,
        onCloseAnimationFinished = screenModel::onCloseAnimationFinished,
        onForwardClick = screenModel::onForwardClick,
        onSaveClick = screenModel::onSaveClick,
        onShareClick = screenModel::onShareClick,
    )
}

@Composable
internal fun PhotoViewerScreenContent(
    modifier: Modifier = Modifier,
    launchRequest: PhotoViewerLaunchRequest,
    uiState: PhotoViewerUiState,
    onPageSettled: (Int) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onEnterImmersiveMode: () -> Unit,
    onMetadataClick: () -> Unit,
    onMetadataDismissed: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseAnimationFinished: () -> Unit,
    onForwardClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    val currentItem = uiState.items.getOrNull(index = uiState.currentPage)
    val actionsEnabled = currentItem != null
    val dismissDragState = rememberPhotoViewerDismissDragState(
        resetKey = photoViewerLaunchRequestKey(launchRequest = launchRequest),
    )
    val scrimColor = MaterialTheme.colorScheme.scrim

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    color = scrimColor.copy(
                        alpha = dismissDragState.backgroundAlpha,
                    ),
                )
            },
    ) {
        PhotoViewerAnimatedContent(
            launchRequest = launchRequest,
            isClosing = uiState.isClosing,
            onCloseAnimationFinished = onCloseAnimationFinished,
        ) {
            PhotoViewerContent(
                uiState = uiState,
                currentItem = currentItem,
                actionsEnabled = actionsEnabled,
                onPageSettled = onPageSettled,
                onToggleDisplayMode = onToggleDisplayMode,
                onEnterImmersiveMode = onEnterImmersiveMode,
                dismissDragState = dismissDragState,
                onMetadataClick = onMetadataClick,
                onCloseClick = onCloseClick,
                onForwardClick = onForwardClick,
                onSaveClick = onSaveClick,
                onShareClick = onShareClick,
            )
        }
    }

    PhotoViewerMetadataSheet(
        isVisible = uiState.isMetadataSheetVisible && currentItem != null,
        item = currentItem,
        actionsEnabled = actionsEnabled,
        onDismissRequest = onMetadataDismissed,
        onSaveClick = onSaveClick,
        onShareClick = onShareClick,
    )
}

@PreviewLightDark
@Composable
private fun PhotoViewerScreenContentPreview() {
    MessagingPreviewTheme {
        PhotoViewerScreenContent(
            launchRequest = PhotoViewerLaunchRequest(
                initialPhotoUri = "content://example/content/1",
                photosUri = "content://example/photos",
                sourceBounds = PhotoViewerSourceBounds(),
            ),
            uiState = PhotoViewerUiState(
                loadState = PhotoViewerLoadState.Loaded,
                items = previewPhotoViewerItems(),
                currentPage = 0,
            ),
            onPageSettled = { _ -> },
            onToggleDisplayMode = {},
            onEnterImmersiveMode = {},
            onMetadataClick = {},
            onMetadataDismissed = {},
            onCloseClick = {},
            onCloseAnimationFinished = {},
            onForwardClick = {},
            onSaveClick = {},
            onShareClick = {},
        )
    }
}
