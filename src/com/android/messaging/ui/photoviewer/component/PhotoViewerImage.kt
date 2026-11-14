package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.messaging.R
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.ui.common.components.mediapreview.mediaReviewPageTransform
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import kotlin.math.roundToInt

private const val PHOTO_VIEWER_DEFAULT_IMAGE_ASPECT_RATIO = 0.75f

private val PhotoViewerCarouselBottomPadding = 96.dp
private val PhotoViewerCarouselTopPadding = 112.dp

@Composable
internal fun PhotoViewerImage(
    item: PhotoViewerItem,
    page: Int,
    pagerState: PagerState,
    displayMode: PhotoViewerDisplayMode,
    immersiveProgress: () -> Float,
    imageSize: IntSize?,
    imageDecodeSize: IntSize,
    onImageLoading: () -> Unit,
    onImageError: () -> Unit,
    onImageLoaded: (IntSize) -> Unit,
) {
    val carouselShape = MaterialTheme.shapes.extraLarge

    Box(
        modifier = Modifier
            .fillMaxSize()
            .photoViewerImageBounds(
                imageAspectRatio = resolvePhotoViewerImageAspectRatio(imageSize = imageSize),
                immersiveProgress = immersiveProgress,
            )
            .photoViewerPageTransform(
                page = page,
                pagerState = pagerState,
                displayMode = displayMode,
            )
            .graphicsLayer {
                shape = carouselShape.scaleCorners(scale = 1f - immersiveProgress())
                clip = true
            },
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

private fun Modifier.photoViewerImageBounds(
    imageAspectRatio: Float,
    immersiveProgress: () -> Float,
): Modifier {
    return layout { measurable, constraints ->
        val progress = immersiveProgress()
        val bounds = resolvePhotoViewerImageBounds(
            containerSize = IntSize(width = constraints.maxWidth, height = constraints.maxHeight),
            imageAspectRatio = imageAspectRatio,
            topPadding = lerp(
                start = PhotoViewerCarouselTopPadding,
                stop = 0.dp,
                fraction = progress,
            ).roundToPx(),
            bottomPadding = lerp(
                start = PhotoViewerCarouselBottomPadding,
                stop = 0.dp,
                fraction = progress,
            ).roundToPx(),
        )
        val placeable = measurable.measure(
            constraints = Constraints.fixed(width = bounds.width, height = bounds.height),
        )

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            placeable.place(position = bounds.topLeft)
        }
    }
}

internal fun resolvePhotoViewerImageBounds(
    containerSize: IntSize,
    imageAspectRatio: Float,
    topPadding: Int,
    bottomPadding: Int,
): IntRect {
    val availableHeight = (containerSize.height - topPadding - bottomPadding)
        .coerceAtLeast(minimumValue = 0)
    val width = minOf(containerSize.width.toFloat(), availableHeight * imageAspectRatio)
    val height = width / imageAspectRatio

    return IntRect(
        offset = IntOffset(
            x = ((containerSize.width - width) / 2f).roundToInt(),
            y = (topPadding + (availableHeight - height) / 2f).roundToInt(),
        ),
        size = IntSize(width = width.roundToInt(), height = height.roundToInt()),
    )
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

private fun CornerBasedShape.scaleCorners(scale: Float): CornerBasedShape {
    return copy(
        topStart = ScaledCornerSize(cornerSize = topStart, scale = scale),
        topEnd = ScaledCornerSize(cornerSize = topEnd, scale = scale),
        bottomEnd = ScaledCornerSize(cornerSize = bottomEnd, scale = scale),
        bottomStart = ScaledCornerSize(cornerSize = bottomStart, scale = scale),
    )
}

private data class ScaledCornerSize(
    private val cornerSize: CornerSize,
    private val scale: Float,
) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float {
        return cornerSize.toPx(shapeSize = shapeSize, density = density) * scale
    }
}
