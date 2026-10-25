@file:JvmName("ParticipantAvatarBitmaps")

package com.android.messaging.ui.common.components.participant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import coil3.SingletonImageLoader
import coil3.executeBlocking
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import coil3.transform.CircleCropTransformation
import com.android.messaging.R
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUriImpl
import com.android.messaging.ui.core.appColorScheme

// The conversation list draws a 20dp letter or icon in its 48dp avatars
private const val FALLBACK_SIZE_FRACTION = 20f / 48f
private const val LABEL_FONT_WEIGHT = 500

internal fun createParticipantAvatarBitmap(
    context: Context,
    avatarUri: Uri?,
    displayName: String?,
    normalizedDestination: String?,
): Bitmap {
    val sizePx = avatarBitmapSize(context = context)
    val photoUri = ResolveAvatarUriImpl().invoke(icon = avatarUri?.toString())
    val photo = photoUri?.let {
        loadAvatarPhoto(context = context, photoUri = it, sizePx = sizePx)
    }

    return photo ?: drawFallbackAvatar(
        context = context,
        sizePx = sizePx,
        fallbackLabel = participantAvatarLabel(source = displayName),
        fallbackIcon = Icons.Default.Person,
        colorSeedCode = participantColorSeed(normalizedDestination = normalizedDestination),
    )
}

internal fun createGroupAvatarBitmap(context: Context): Bitmap {
    return drawFallbackAvatar(
        context = context,
        sizePx = avatarBitmapSize(context = context),
        fallbackLabel = null,
        fallbackIcon = Icons.Default.Group,
        colorSeedCode = null,
    )
}

private fun avatarBitmapSize(context: Context): Int {
    return context.resources.getDimensionPixelSize(R.dimen.contact_icon_view_normal_size)
}

private fun loadAvatarPhoto(context: Context, photoUri: String, sizePx: Int): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(photoUri)
        .size(sizePx)
        .scale(Scale.FILL)
        .precision(Precision.EXACT)
        .transformations(CircleCropTransformation())
        .allowHardware(false)
        .build()
    val result = SingletonImageLoader.get(context).executeBlocking(request)

    return (result as? SuccessResult)?.image?.toBitmap(width = sizePx, height = sizePx)
}

private fun drawFallbackAvatar(
    context: Context,
    sizePx: Int,
    fallbackLabel: String?,
    fallbackIcon: ImageVector,
    colorSeedCode: String?,
): Bitmap {
    val fallbackColors = fallbackColors(context = context, colorSeedCode = colorSeedCode)
    val bitmap = createBitmap(width = sizePx, height = sizePx)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fallbackColors.background.toArgb()
    }
    canvas.drawCircle(radius, radius, radius, paint)

    paint.color = fallbackColors.content.toArgb()
    val fallbackSizePx = sizePx * FALLBACK_SIZE_FRACTION
    when (fallbackLabel) {
        null -> canvas.drawFallbackIcon(
            icon = fallbackIcon,
            iconSizePx = fallbackSizePx,
            paint = paint,
        )

        else -> canvas.drawFallbackLabel(
            label = fallbackLabel,
            textSizePx = fallbackSizePx,
            paint = paint,
        )
    }

    return bitmap
}

private fun fallbackColors(
    context: Context,
    colorSeedCode: String?,
): ParticipantAvatarFallbackColors {
    val isDarkTheme = context.resources.configuration.isNightModeActive

    return when {
        colorSeedCode.isNullOrBlank() -> {
            val colorScheme = appColorScheme(context = context, isDarkTheme = isDarkTheme)
            ParticipantAvatarFallbackColors(
                background = colorScheme.primaryContainer,
                content = colorScheme.onPrimaryContainer,
            )
        }

        else -> participantAvatarFallbackColors(
            colorSeedCode = colorSeedCode,
            isDarkTheme = isDarkTheme,
        )
    }
}

private fun Canvas.drawFallbackLabel(label: String, textSizePx: Float, paint: Paint) {
    paint.typeface = Typeface.create(Typeface.DEFAULT, LABEL_FONT_WEIGHT, false)
    paint.textSize = textSizePx
    paint.textAlign = Paint.Align.CENTER
    val fontMetrics = paint.fontMetrics
    val baseline = height / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f

    drawText(label, width / 2f, baseline, paint)
}

private fun Canvas.drawFallbackIcon(icon: ImageVector, iconSizePx: Float, paint: Paint) {
    val offset = (width - iconSizePx) / 2f

    withTranslation(x = offset, y = offset) {
        scale(iconSizePx / icon.viewportWidth, iconSizePx / icon.viewportHeight)
        icon.root.filterIsInstance<VectorPath>().forEach { vectorPath ->
            drawPath(vectorPath.pathData.toPath().asAndroidPath(), paint)
        }
    }
}
