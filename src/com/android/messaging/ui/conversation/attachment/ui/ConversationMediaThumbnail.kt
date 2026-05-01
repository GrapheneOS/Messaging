package com.android.messaging.ui.conversation.attachment.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.messaging.util.ContentType

private const val THUMBNAIL_FADE_IN_DURATION_MILLIS = 90

@Composable
internal fun ConversationMediaThumbnail(
    modifier: Modifier = Modifier,
    contentUri: String,
    contentType: String,
    size: IntSize,
    contentScale: ContentScale = ContentScale.Crop,
    crossfadeEnabled: Boolean = true,
    backgroundColor: Color = Color.Unspecified,
    useBitmapLoader: Boolean = false,
    softenBitmap: Boolean = false,
) {
    val context = LocalContext.current

    val contentUriAsUri = rememberContentUri(contentUri = contentUri)
    val normalizedSize = remember(size) {
        size.sanitized()
    }

    val resolvedBackgroundColor = resolveBackgroundColor(backgroundColor = backgroundColor)

    val shouldUseCoilImageLoader = shouldUseCoilImageLoader(
        contentType = contentType,
        useBitmapLoader = useBitmapLoader,
    )

    when {
        shouldUseCoilImageLoader -> {
            CoilThumbnail(
                modifier = modifier,
                context = context,
                contentUri = contentUriAsUri,
                size = normalizedSize,
                contentScale = contentScale,
                crossfadeEnabled = crossfadeEnabled,
            )
        }

        else -> {
            BitmapThumbnail(
                modifier = modifier,
                contentUri = contentUriAsUri,
                contentType = contentType,
                size = normalizedSize,
                contentScale = contentScale,
                crossfadeEnabled = crossfadeEnabled,
                backgroundColor = resolvedBackgroundColor,
                softenBitmap = softenBitmap,
                useBitmapLoader = useBitmapLoader,
            )
        }
    }
}

@Composable
internal fun rememberConversationMediaThumbnailBitmap(
    contentUri: Uri,
    contentType: String,
    size: IntSize,
    softenBitmap: Boolean = false,
): Bitmap? {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        contentUri,
        contentType,
        size,
        softenBitmap,
    ) {
        value = loadConversationMediaThumbnailBitmap(
            contentResolver = context.contentResolver,
            contentUri = contentUri,
            contentType = contentType,
            size = size,
            softenBitmap = softenBitmap,
        )
    }

    return bitmap
}

@Composable
private fun BitmapThumbnail(
    modifier: Modifier,
    contentUri: Uri,
    contentType: String,
    size: IntSize,
    contentScale: ContentScale,
    crossfadeEnabled: Boolean,
    backgroundColor: Color,
    softenBitmap: Boolean,
    useBitmapLoader: Boolean,
) {
    val bitmap = rememberConversationMediaThumbnailBitmap(
        contentUri = contentUri,
        contentType = contentType,
        size = size,
        softenBitmap = softenBitmap,
    )
    val bitmapAlpha = rememberThumbnailAlpha(
        crossfadeEnabled = crossfadeEnabled,
        isLoaded = bitmap != null,
        animationLabel = "conversationMediaThumbnailBitmapAlpha",
    )
    val filterQuality = resolveBitmapFilterQuality(useBitmapLoader = useBitmapLoader)

    Box(modifier = modifier) {
        ThumbnailPlaceholder(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = backgroundColor,
        )

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                filterQuality = filterQuality,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha = bitmapAlpha),
            )
        }
    }
}

@Composable
private fun CoilThumbnail(
    modifier: Modifier,
    context: Context,
    contentUri: Uri,
    size: IntSize,
    contentScale: ContentScale,
    crossfadeEnabled: Boolean,
) {
    val imageRequest = remember(
        context,
        contentUri,
        size,
    ) {
        ImageRequest.Builder(context)
            .data(contentUri)
            .size(width = size.width, height = size.height)
            .build()
    }

    val isImageLoaded = remember(contentUri, size, crossfadeEnabled) {
        mutableStateOf(value = !crossfadeEnabled)
    }

    val visibilityAlpha = rememberThumbnailAlpha(
        crossfadeEnabled = crossfadeEnabled,
        isLoaded = isImageLoaded.value,
        animationLabel = "conversationMediaThumbnailImageAlpha",
    )

    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = contentScale,
        filterQuality = FilterQuality.Low,
        modifier = modifier.alpha(alpha = visibilityAlpha),
        onError = {
            isImageLoaded.value = true
        },
        onSuccess = {
            isImageLoaded.value = true
        },
    )
}

@Composable
private fun ThumbnailPlaceholder(
    modifier: Modifier,
    backgroundColor: Color,
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
        }
    }
}

@Composable
private fun rememberContentUri(
    contentUri: String,
): Uri {
    return remember(contentUri) {
        contentUri.toUri()
    }
}

@Composable
private fun rememberThumbnailAlpha(
    crossfadeEnabled: Boolean,
    isLoaded: Boolean,
    animationLabel: String,
): Float {
    val targetAlpha = when {
        !crossfadeEnabled || isLoaded -> 1f
        else -> 0f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = THUMBNAIL_FADE_IN_DURATION_MILLIS),
        label = animationLabel,
    )

    return animatedAlpha
}

@Composable
private fun resolveBackgroundColor(
    backgroundColor: Color,
): Color {
    return when {
        backgroundColor != Color.Unspecified -> backgroundColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

private fun shouldUseCoilImageLoader(
    contentType: String,
    useBitmapLoader: Boolean,
): Boolean {
    return ContentType.isImageType(contentType) && !useBitmapLoader
}

private fun resolveBitmapFilterQuality(useBitmapLoader: Boolean): FilterQuality {
    return when {
        useBitmapLoader -> FilterQuality.Medium
        else -> FilterQuality.Low
    }
}
