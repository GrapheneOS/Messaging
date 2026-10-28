package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.bottomBarInsets
import com.android.messaging.ui.common.components.horizontalSafeDrawingInsets
import com.android.messaging.ui.core.MessagingPreviewTheme

private const val CAMERA_PREVIEW_HEIGHT_FRACTION = 2f / 3f
private const val PHOTO_PICKER_PANE_WIDTH_FRACTION = 0.45f
private val PICKER_GALLERY_SHEET_HEIGHT_REDUCTION = 16.dp
private val PHOTO_PICKER_SHEET_TOP_CORNER_RADIUS = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationMediaPickerSheetScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState,
    photoPickerContent: @Composable (isBesideContent: Boolean) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize(),
    ) {
        val isPhotoPickerBesideContent = maxWidth > maxHeight
        val sheetState = scaffoldState.bottomSheetState
        val statusBarInsets = WindowInsets.statusBars
        val density = LocalDensity.current
        val expansionProgress = {
            density.photoPickerSheetExpansionProgress(
                sheetOffset = sheetState.requireOffset(),
                statusBarInsets = statusBarInsets,
            )
        }
        val sheetDragHandle: @Composable (() -> Unit)? = when {
            isPhotoPickerBesideContent -> null
            else -> {
                { ConversationPhotoPickerSheetHeader(expansionProgress = expansionProgress) }
            }
        }
        val isPhotoPickerShown = rememberIsPhotoPickerPlacementSettled(isPhotoPickerBesideContent)

        // A wide layout keeps the scaffold with an empty sheet instead of dropping it, so the
        // camera and review keep their state when the device rotates.
        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetContainerColor = Color.Transparent,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetShape = RectangleShape,
            containerColor = Color.Transparent,
            sheetDragHandle = sheetDragHandle,
            sheetSwipeEnabled = !isPhotoPickerBesideContent,
            sheetPeekHeight = when {
                isPhotoPickerBesideContent -> 0.dp
                else -> calculatePhotoPickerSheetPeekHeight(maxHeight = maxHeight)
            },
            sheetContent = {
                Box(modifier = Modifier.excludeStatusBarHeight(statusBarInsets = statusBarInsets)) {
                    if (isPhotoPickerShown && !isPhotoPickerBesideContent) {
                        photoPickerContent(false)
                    }
                }
            },
        ) { innerPadding ->
            ConversationMediaPickerSheetBody(
                isPhotoPickerBesideContent = isPhotoPickerBesideContent,
                isPhotoPickerShown = isPhotoPickerShown,
                sheetContentPadding = innerPadding,
                photoPickerContent = photoPickerContent,
                content = content,
            )
        }

        if (!isPhotoPickerBesideContent) {
            ConversationPhotoPickerStatusBarCover(
                statusBarInsets = statusBarInsets,
                expansionProgress = expansionProgress,
            )
        }
    }
}

// The sheet and the pane pickers share one picker state, and a picker clears the state's surface
// when it detaches. Keeping the new place empty until the old picker has left stops it clearing
// the surface the new picker opens its session on, which crashed on rotation.
@Composable
private fun rememberIsPhotoPickerPlacementSettled(isBesideContent: Boolean): Boolean {
    var settledIsBesideContent by remember { mutableStateOf(value = isBesideContent) }

    LaunchedEffect(isBesideContent) {
        settledIsBesideContent = isBesideContent
    }

    return settledIsBesideContent == isBesideContent
}

@Composable
private fun ConversationMediaPickerSheetBody(
    isPhotoPickerBesideContent: Boolean,
    isPhotoPickerShown: Boolean,
    sheetContentPadding: PaddingValues,
    photoPickerContent: @Composable (isBesideContent: Boolean) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .fillMaxHeight(),
        ) {
            content(
                when {
                    isPhotoPickerBesideContent -> besidePhotoPickerContentPadding()
                    else -> sheetContentPadding
                },
            )
        }

        if (isPhotoPickerBesideContent) {
            ConversationPhotoPickerPane(
                modifier = Modifier
                    .fillMaxWidth(fraction = PHOTO_PICKER_PANE_WIDTH_FRACTION)
                    .fillMaxHeight(),
            ) {
                if (isPhotoPickerShown) {
                    photoPickerContent(true)
                }
            }
        }
    }
}

// Covers the status bar above the expanded sheet, so the camera doesn't show there.
@Composable
private fun ConversationPhotoPickerStatusBarCover(
    statusBarInsets: WindowInsets,
    expansionProgress: () -> Float,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(insets = statusBarInsets)
            .drawBehind { drawRect(color = surfaceColor, alpha = expansionProgress()) },
    )
}

@Composable
private fun besidePhotoPickerContentPadding(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current

    return PaddingValues(
        start = horizontalSafeDrawingInsets().calculateStartPadding(layoutDirection),
        bottom = bottomBarInsets().asPaddingValues().calculateBottomPadding(),
    )
}

@Composable
private fun ConversationPhotoPickerPane(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(
                end = horizontalSafeDrawingInsets().calculateEndPadding(layoutDirection),
            ),
    ) {
        content()
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
        ConversationMediaPickerSheetScaffoldPreviewContent()
    }
}

@Preview(widthDp = 891, heightDp = 411)
@Composable
private fun ConversationMediaPickerSheetScaffoldLandscapePreview() {
    MessagingPreviewTheme {
        ConversationMediaPickerSheetScaffoldPreviewContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationMediaPickerSheetScaffoldPreviewContent() {
    ConversationMediaPickerSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(),
        photoPickerContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
