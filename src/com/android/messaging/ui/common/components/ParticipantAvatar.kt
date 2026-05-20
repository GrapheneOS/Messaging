package com.android.messaging.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

@Composable
internal fun ParticipantAvatar(
    avatarUri: String?,
    fallbackIcon: ImageVector,
    fallbackIconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            avatarUri.isNullOrBlank() -> {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    modifier = Modifier.size(fallbackIconSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            else -> {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
internal fun ParticipantAvatar(
    avatarUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallbackIconSize: Dp = size / 2,
    fallbackIcon: ImageVector = Icons.Default.Person,
) {
    ParticipantAvatar(
        avatarUri = avatarUri,
        fallbackIcon = fallbackIcon,
        fallbackIconSize = fallbackIconSize,
        modifier = modifier.size(size),
    )
}
