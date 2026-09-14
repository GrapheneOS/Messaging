package com.android.messaging.ui.common.components.participant

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.android.messaging.sms.MmsSmsUtils

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
    ParticipantAvatar(
        avatarImage = avatarUri
            ?.takeIf { it.isNotBlank() }
            ?.let(ParticipantAvatarImage::Uri),
        fallbackIcon = fallbackIcon,
        fallbackSize = fallbackSize,
        fallbackLabel = fallbackLabel,
        modifier = modifier,
        colorSeedCode = colorSeedCode,
        shape = shape,
        isSelected = isSelected,
    )
}

@Composable
internal fun ParticipantAvatar(
    avatarImage: ParticipantAvatarImage?,
    fallbackIcon: ImageVector,
    fallbackSize: Dp,
    fallbackLabel: String?,
    modifier: Modifier = Modifier,
    colorSeedCode: String? = null,
    shape: Shape = CircleShape,
    isSelected: Boolean = false,
) {
    val fallbackColors = resolvedFallbackColors(
        colorSeedCode = colorSeedCode,
        isSelected = isSelected,
    )

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

            avatarImage != null -> {
                AsyncImage(
                    model = when (avatarImage) {
                        is ParticipantAvatarImage.Bytes -> avatarImage.value
                        is ParticipantAvatarImage.Uri -> avatarImage.value
                    },
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
    val trimmedSource = source
        ?.trim()
        ?.dropWhile(::isFormatChar)

    return when {
        trimmedSource.isNullOrBlank() -> null
        MmsSmsUtils.isPhoneNumber(trimmedSource) -> null
        else -> trimmedSource.first().uppercaseChar().toString()
    }
}

private fun isFormatChar(char: Char): Boolean {
    return char.category == CharCategory.FORMAT
}
