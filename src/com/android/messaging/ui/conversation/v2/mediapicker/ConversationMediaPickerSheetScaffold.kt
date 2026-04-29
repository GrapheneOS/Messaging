package com.android.messaging.ui.conversation.v2.mediapicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize(),
            scaffoldState = scaffoldState,
            sheetContainerColor = Color.Transparent,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetShape = RectangleShape,
            containerColor = Color.Transparent,
            sheetDragHandle = {
                ConversationPhotoPickerSheetHeader()
            },
            sheetPeekHeight = calculatePhotoPickerSheetPeekHeight(maxHeight = maxHeight),
            sheetContent = {
                photoPickerSheetContent()
            },
        ) { innerPadding ->
            content(innerPadding)
        }
    }
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
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(
                    topStart = PHOTO_PICKER_SHEET_TOP_CORNER_RADIUS,
                    topEnd = PHOTO_PICKER_SHEET_TOP_CORNER_RADIUS,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        BottomSheetDefaults.DragHandle()
    }
}
