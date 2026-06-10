package com.android.messaging.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.android.messaging.sms.MmsSmsUtils
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f
private const val FULL_HUE_CIRCLE_DEGREES = 360f
private const val HUE_SEGMENT_DEGREES = 60f
private const val GOLDEN_ANGLE_DEGREES = 137.508f
private const val BYTE_COLOR_MAX_VALUE = 255
private const val FNV_OFFSET_BASIS = -0x7ee3623b
private const val FNV_PRIME = 0x01000193

private const val LIGHT_THEME_AVATAR_BACKGROUND_SATURATION = 0.58f
private const val LIGHT_THEME_AVATAR_BACKGROUND_LIGHTNESS = 0.82f
private const val LIGHT_THEME_AVATAR_CONTENT_SATURATION = 0.82f
private const val LIGHT_THEME_AVATAR_CONTENT_LIGHTNESS = 0.22f

private const val DARK_THEME_AVATAR_BACKGROUND_SATURATION = 0.48f
private const val DARK_THEME_AVATAR_BACKGROUND_LIGHTNESS = 0.30f
private const val DARK_THEME_AVATAR_CONTENT_SATURATION = 0.70f
private const val DARK_THEME_AVATAR_CONTENT_LIGHTNESS = 0.88f

@Composable
internal fun ParticipantAvatar(
    avatarUri: String?,
    fallbackIcon: ImageVector,
    fallbackSize: Dp,
    fallbackLabel: String?,
    modifier: Modifier = Modifier,
    colorSeedCode: String? = null,
    shape: Shape = CircleShape,
    isSelected: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
    val fallbackColors = when {
        isSelected -> ParticipantAvatarFallbackColors(
            background = colorScheme.primary,
            content = colorScheme.onPrimary,
        )

        colorSeedCode.isNullOrBlank() -> ParticipantAvatarFallbackColors(
            background = colorScheme.primaryContainer,
            content = colorScheme.onPrimaryContainer,
        )

        else -> participantAvatarFallbackColors(
            colorSeedCode = colorSeedCode,
            isDarkTheme = isDarkTheme,
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fallbackColors.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isSelected -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(fallbackSize),
                    tint = fallbackColors.content,
                )
            }

            !avatarUri.isNullOrBlank() -> {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            !fallbackLabel.isNullOrBlank() -> {
                val fontSize = with(LocalDensity.current) { fallbackSize.toSp() }

                Text(
                    text = fallbackLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    color = fallbackColors.content,
                )
            }

            else -> {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    modifier = Modifier.size(fallbackSize),
                    tint = fallbackColors.content,
                )
            }
        }
    }
}

@Composable
internal fun ParticipantAvatar(
    avatarUri: String?,
    size: Dp,
    fallbackLabel: String?,
    modifier: Modifier = Modifier,
    colorSeedCode: String? = null,
    fallbackSize: Dp = size / 2,
    fallbackIcon: ImageVector = Icons.Default.Person,
    shape: Shape = CircleShape,
    isSelected: Boolean = false,
) {
    ParticipantAvatar(
        avatarUri = avatarUri,
        fallbackIcon = fallbackIcon,
        fallbackSize = fallbackSize,
        fallbackLabel = fallbackLabel,
        modifier = modifier.size(size),
        colorSeedCode = colorSeedCode,
        shape = shape,
        isSelected = isSelected,
    )
}

internal fun participantColorSeed(normalizedDestination: String?): String? {
    return normalizedDestination
        ?.filterNot { it.isWhitespace() }
        ?.takeIf { it.isNotEmpty() }
}

internal fun participantAvatarLabel(source: String?): String? {
    val trimmedSource = source?.trim()

    return when {
        trimmedSource.isNullOrBlank() -> null
        MmsSmsUtils.isPhoneNumber(trimmedSource) -> null
        else -> trimmedSource.first().uppercaseChar().toString()
    }
}

internal fun participantAvatarFallbackColors(
    colorSeedCode: String,
    isDarkTheme: Boolean,
): ParticipantAvatarFallbackColors {
    val hue = participantAvatarHue(colorSeedCode = colorSeedCode)

    return when {
        isDarkTheme -> ParticipantAvatarFallbackColors(
            background = hslColor(
                hue = hue,
                saturation = DARK_THEME_AVATAR_BACKGROUND_SATURATION,
                lightness = DARK_THEME_AVATAR_BACKGROUND_LIGHTNESS,
            ),
            content = hslColor(
                hue = hue,
                saturation = DARK_THEME_AVATAR_CONTENT_SATURATION,
                lightness = DARK_THEME_AVATAR_CONTENT_LIGHTNESS,
            ),
        )

        else -> ParticipantAvatarFallbackColors(
            background = hslColor(
                hue = hue,
                saturation = LIGHT_THEME_AVATAR_BACKGROUND_SATURATION,
                lightness = LIGHT_THEME_AVATAR_BACKGROUND_LIGHTNESS,
            ),
            content = hslColor(
                hue = hue,
                saturation = LIGHT_THEME_AVATAR_CONTENT_SATURATION,
                lightness = LIGHT_THEME_AVATAR_CONTENT_LIGHTNESS,
            ),
        )
    }
}

internal data class ParticipantAvatarFallbackColors(
    val background: Color,
    val content: Color,
)

private fun participantAvatarHue(colorSeedCode: String): Float {
    val positiveHash = colorSeedCode.stableHashCode() and Int.MAX_VALUE

    return (positiveHash * GOLDEN_ANGLE_DEGREES) % FULL_HUE_CIRCLE_DEGREES
}

private fun String.stableHashCode(): Int {
    var hash = FNV_OFFSET_BASIS

    forEach { character ->
        hash = hash xor character.code
        hash *= FNV_PRIME
    }

    return hash
}

private fun hslColor(
    hue: Float,
    saturation: Float,
    lightness: Float,
): Color {
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val huePrime = hue / HUE_SEGMENT_DEGREES
    val secondLargestComponent = chroma * (1f - abs(huePrime % 2f - 1f))
    val lightnessMatch = lightness - chroma / 2f

    val sextantComponents = listOf(
        Triple(chroma, secondLargestComponent, 0f),
        Triple(secondLargestComponent, chroma, 0f),
        Triple(0f, chroma, secondLargestComponent),
        Triple(0f, secondLargestComponent, chroma),
        Triple(secondLargestComponent, 0f, chroma),
        Triple(chroma, 0f, secondLargestComponent),
    )
    val sextant = huePrime.toInt().coerceIn(0, sextantComponents.lastIndex)
    val (redPrime, greenPrime, bluePrime) = sextantComponents[sextant]

    return Color(
        red = (redPrime + lightnessMatch).toByteColorComponent(),
        green = (greenPrime + lightnessMatch).toByteColorComponent(),
        blue = (bluePrime + lightnessMatch).toByteColorComponent(),
    )
}

private fun Float.toByteColorComponent(): Int {
    return (coerceIn(minimumValue = 0f, maximumValue = 1f) * BYTE_COLOR_MAX_VALUE)
        .roundToInt()
}
