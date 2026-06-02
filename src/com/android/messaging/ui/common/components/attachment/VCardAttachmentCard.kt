package com.android.messaging.ui.common.components.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.android.messaging.util.UriUtil

private val VCARD_AVATAR_SIZE = 36.dp
private val VCARD_AVATAR_ICON_SIZE = 20.dp

internal enum class VCardAttachmentKind {
    Contact,
    Location,
}

@Composable
internal fun VCardAttachmentCard(
    kind: VCardAttachmentKind,
    avatarUri: String?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VCardAttachmentLeadingVisual(
            kind = kind,
            avatarUri = avatarUri,
            title = title,
        )

        Column(
            modifier = Modifier.weight(weight = 1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            subtitle?.let { subtitleText ->
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun VCardAttachmentLeadingVisual(
    kind: VCardAttachmentKind,
    avatarUri: String?,
    title: String,
) {
    when (kind) {
        VCardAttachmentKind.Contact -> {
            VCardAttachmentAvatar(
                avatarUri = avatarUri,
                title = title,
            )
        }

        VCardAttachmentKind.Location -> {
            Box(
                modifier = Modifier
                    .size(size = VCARD_AVATAR_SIZE),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(size = VCARD_AVATAR_ICON_SIZE),
                    imageVector = Icons.Rounded.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VCardAttachmentAvatar(
    avatarUri: String?,
    title: String,
) {
    val displayableAvatarUri = remember(avatarUri) {
        displayableVCardAvatarUri(avatarUri = avatarUri)
    }

    val label = remember(title) {
        vCardAvatarLabel(title = title)
    }

    Box(
        modifier = Modifier
            .size(size = VCARD_AVATAR_SIZE)
            .clip(shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        VCardAttachmentAvatarFallback(label = label)

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
private fun VCardAttachmentAvatarFallback(
    label: String?,
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
                        modifier = Modifier.size(size = VCARD_AVATAR_ICON_SIZE),
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                    )
                }

                else -> {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
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

private fun vCardAvatarLabel(title: String): String? {
    return title
        .trim()
        .takeIf { it.isNotBlank() }
        ?.first()
        ?.uppercaseChar()
        ?.toString()
}
