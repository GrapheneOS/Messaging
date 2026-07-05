package com.android.messaging.ui.common.components.attachment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.android.messaging.util.UriUtil

@Composable
internal fun VCardAvatar(
    avatarUri: String?,
    avatarName: String?,
    size: Dp,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    val displayableAvatarUri = remember(avatarUri) {
        displayableVCardAvatarUri(avatarUri = avatarUri)
    }

    val label = remember(avatarName) {
        vCardAvatarLabel(avatarName = avatarName)
    }

    Box(
        modifier = modifier
            .size(size = size)
            .clip(shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        VCardAvatarFallback(
            label = label,
            iconSize = iconSize,
        )

        displayableAvatarUri?.let { uri ->
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun VCardAvatarFallback(
    label: String?,
    iconSize: Dp,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (label) {
                null -> {
                    Icon(
                        modifier = Modifier.size(size = iconSize),
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                    )
                }

                else -> {
                    val fontSize = with(LocalDensity.current) { iconSize.toSp() }

                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = fontSize,
                        lineHeight = fontSize,
                    )
                }
            }
        }
    }
}

private fun displayableVCardAvatarUri(avatarUri: String?): String? {
    return avatarUri
        ?.takeIf { it.isNotBlank() }
        ?.toUri()
        ?.takeIf(UriUtil::isLocalResourceUri)
        ?.toString()
}

private fun vCardAvatarLabel(avatarName: String?): String? {
    return avatarName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.first()
        ?.uppercaseChar()
        ?.toString()
}
