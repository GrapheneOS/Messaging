package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.messaging.R
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.common.components.mediapreview.mediaReviewPageTransform
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode

private const val PHOTO_VIEWER_DEFAULT_IMAGE_ASPECT_RATIO = 0.75f

private val PhotoViewerCarouselBottomPadding = 96.dp
private val PhotoViewerCarouselTopPadding = 112.dp

@Composable
internal fun PhotoViewerImage(
    item: PhotoViewerItem,
    page: Int,
    pagerState: PagerState,
    displayMode: PhotoViewerDisplayMode,
    imageSize: IntSize?,
    imageDecodeSize: IntSize,
    onImageLoading: () -> Unit,
    onImageError: () -> Unit,
    onImageLoaded: (IntSize) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = when (displayMode) {
                    PhotoViewerDisplayMode.Carousel -> PhotoViewerCarouselTopPadding
                    PhotoViewerDisplayMode.Immersive -> 0.dp
                },
                bottom = when (displayMode) {
                    PhotoViewerDisplayMode.Carousel -> PhotoViewerCarouselBottomPadding
                    PhotoViewerDisplayMode.Immersive -> 0.dp
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val imageLayout = rememberPhotoViewerImageLayout(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            imageSize = imageSize,
        )

        Surface(
            modifier = Modifier
                .width(width = imageLayout.width)
                .height(height = imageLayout.height)
                .photoViewerPageTransform(
                    page = page,
                    pagerState = pagerState,
                    displayMode = displayMode,
                ),
            shape = when (displayMode) {
                PhotoViewerDisplayMode.Carousel -> MaterialTheme.shapes.extraLarge
                PhotoViewerDisplayMode.Immersive -> RectangleShape
            },
            color = Color.Transparent,
        ) {
            PhotoViewerAsyncImage(
                item = item,
                imageDecodeSize = imageDecodeSize,
                onImageLoading = onImageLoading,
                onImageError = onImageError,
                onImageLoaded = onImageLoaded,
            )
        }
    }
}

@Composable
private fun PhotoViewerAsyncImage(
    item: PhotoViewerItem,
    imageDecodeSize: IntSize,
    onImageLoading: () -> Unit,
    onImageError: () -> Unit,
    onImageLoaded: (IntSize) -> Unit,
) {
    val imageRequest = rememberPhotoViewerImageRequest(
        item = item,
        imageDecodeSize = imageDecodeSize,
    )

    AsyncImage(
        model = imageRequest,
        contentDescription = stringResource(
            id = R.string.photo_viewer_image_content_description,
        ),
        contentScale = ContentScale.Fit,
        onError = {
            onImageError()
        },
        onLoading = {
            onImageLoading()
        },
        onSuccess = { state ->
            val loadedImage = state.result.image
            onImageLoaded(
                IntSize(
                    width = loadedImage.width.coerceAtLeast(minimumValue = 1),
                    height = loadedImage.height.coerceAtLeast(minimumValue = 1),
                ),
            )
        },
        modifier = Modifier
            .fillMaxSize(),
    )
}

@Composable
private fun rememberPhotoViewerImageRequest(
    item: PhotoViewerItem,
    imageDecodeSize: IntSize,
): ImageRequest {
    val context = LocalContext.current
    return remember(
        context,
        item.contentUri,
        imageDecodeSize,
    ) {
        ImageRequest.Builder(context)
            .data(data = item.contentUri)
            .size(
                width = imageDecodeSize.width,
                height = imageDecodeSize.height,
            )
            .build()
    }
}

@Composable
private fun rememberPhotoViewerImageLayout(
    maxWidth: Dp,
    maxHeight: Dp,
    imageSize: IntSize?,
): PhotoViewerImageLayout {
    return remember(maxWidth, maxHeight, imageSize) {
        val aspectRatio = resolvePhotoViewerImageAspectRatio(imageSize = imageSize)
        val widthFromHeight = maxHeight * aspectRatio

        val width = when {
            maxWidth <= widthFromHeight -> maxWidth
            else -> widthFromHeight
        }

        PhotoViewerImageLayout(
            width = width,
            height = width / aspectRatio,
        )
    }
}

private fun resolvePhotoViewerImageAspectRatio(imageSize: IntSize?): Float {
    return when {
        imageSize == null -> PHOTO_VIEWER_DEFAULT_IMAGE_ASPECT_RATIO
        imageSize.width <= 0 -> PHOTO_VIEWER_DEFAULT_IMAGE_ASPECT_RATIO
        imageSize.height <= 0 -> PHOTO_VIEWER_DEFAULT_IMAGE_ASPECT_RATIO
        else -> {
            imageSize.width.toFloat() / imageSize.height.toFloat()
        }
    }
}

private fun Modifier.photoViewerPageTransform(
    page: Int,
    pagerState: PagerState,
    displayMode: PhotoViewerDisplayMode,
): Modifier {
    return when (displayMode) {
        PhotoViewerDisplayMode.Carousel -> {
            mediaReviewPageTransform(
                page = page,
                pagerState = pagerState,
                removalProgress = 1f,
            )
        }

        PhotoViewerDisplayMode.Immersive -> this
    }
}

private data class PhotoViewerImageLayout(
    val width: Dp,
    val height: Dp,
)
