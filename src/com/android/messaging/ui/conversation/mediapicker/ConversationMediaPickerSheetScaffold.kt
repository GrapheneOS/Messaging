package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewTheme

private const val CAMERA_PREVIEW_HEIGHT_FRACTION = 2f / 3f
private val PICKER_GALLERY_SHEET_HEIGHT_REDUCTION = 16.dp
private val PHOTO_PICKER_SHEET_TOP_CORNER_RADIUS = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationMediaPickerSheetScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState,
    photoPickerSheetContent: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val sheetState = scaffoldState.bottomSheetState
        val statusBarInsets = WindowInsets.statusBars
        val density = LocalDensity.current
        val expansionProgress = {
            density.photoPickerSheetExpansionProgress(
                sheetOffset = sheetState.requireOffset(),
                statusBarInsets = statusBarInsets,
            )
        }

        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetContainerColor = Color.Transparent,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetShape = RectangleShape,
            containerColor = Color.Transparent,
            sheetDragHandle = {
                ConversationPhotoPickerSheetHeader(expansionProgress = expansionProgress)
            },
            sheetPeekHeight = calculatePhotoPickerSheetPeekHeight(maxHeight = maxHeight),
            sheetContent = {
                Box(modifier = Modifier.excludeStatusBarHeight(statusBarInsets = statusBarInsets)) {
                    photoPickerSheetContent()
                }
            },
        ) { innerPadding ->
            content(innerPadding)
        }

        // Covers the status bar above the expanded sheet, so the camera doesn't show there.
        val surfaceColor = MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(insets = statusBarInsets)
                .drawBehind { drawRect(color = surfaceColor, alpha = expansionProgress()) },
        )
    }
}

// Keeps the expanded sheet below the status bar, so its drag handle stays reachable.
private fun Modifier.excludeStatusBarHeight(statusBarInsets: WindowInsets): Modifier {
    return layout { measurable, constraints ->
        val maxHeight = when {
            constraints.hasBoundedHeight -> {
                (constraints.maxHeight - statusBarInsets.getTop(this))
                    .coerceAtLeast(constraints.minHeight)
            }

            else -> constraints.maxHeight
        }
        val placeable = measurable.measure(constraints.copy(maxHeight = maxHeight))

        layout(width = placeable.width, height = placeable.height) {
            placeable.place(x = 0, y = 0)
        }
    }
}

private fun Density.photoPickerSheetExpansionProgress(
    sheetOffset: Float,
    statusBarInsets: WindowInsets,
): Float {
    val distanceToExpanded = sheetOffset - statusBarInsets.getTop(this)

    return (1f - distanceToExpanded / PHOTO_PICKER_SHEET_TOP_CORNER_RADIUS.toPx()).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}

private fun calculatePhotoPickerSheetPeekHeight(maxHeight: Dp): Dp {
    val previewHeight = maxHeight * CAMERA_PREVIEW_HEIGHT_FRACTION
    val defaultSheetPeekHeight = maxHeight - previewHeight

    return when {
        defaultSheetPeekHeight > PICKER_GALLERY_SHEET_HEIGHT_REDUCTION -> {
            defaultSheetPeekHeight - PICKER_GALLERY_SHEET_HEIGHT_REDUCTION
        }

        else -> defaultSheetPeekHeight
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationPhotoPickerSheetHeader(
    modifier: Modifier = Modifier,
    expansionProgress: () -> Float,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Squares the corners as the sheet expands, so the camera doesn't show past them.
                val cornerRadius = PHOTO_PICKER_SHEET_TOP_CORNER_RADIUS.toPx() *
                    (1f - expansionProgress())

                val shape = RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = cornerRadius,
                )

                drawOutline(
                    outline = shape.createOutline(
                        size = size,
                        layoutDirection = layoutDirection,
                        density = this,
                    ),
                    color = surfaceColor,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun ConversationMediaPickerSheetScaffoldPreview() {
    MessagingPreviewTheme {
        ConversationMediaPickerSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(),
            photoPickerSheetContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Photo picker")
                }
            },
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.scrim),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Camera preview",
                    color = Color.White,
                )
            }
        }
    }
}
