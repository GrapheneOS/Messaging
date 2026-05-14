package com.android.messaging.ui.conversation.attachment.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.util.UriUtil

private val VCARD_AVATAR_SIZE = 36.dp
private val VCARD_AVATAR_ICON_SIZE = 20.dp

@Composable
internal fun ConversationVCardAttachmentCardContent(
    modifier: Modifier = Modifier,
    type: ConversationVCardAttachmentType,
    avatarUri: String?,
    titleText: String?,
    titleTextResId: Int?,
    subtitleText: String?,
    subtitleTextResId: Int?,
) {
    val title = resolveTitleText(
        titleText = titleText,
        titleTextResId = titleTextResId,
    )

    val subtitle = resolveSubtitleText(
        subtitleText = subtitleText,
        subtitleTextResId = subtitleTextResId,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationVCardAttachmentLeadingVisual(
            type = type,
            avatarUri = avatarUri,
            titleText = titleText,
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
private fun ConversationVCardAttachmentLeadingVisual(
    type: ConversationVCardAttachmentType,
    avatarUri: String?,
    titleText: String?,
) {
    when (type) {
        ConversationVCardAttachmentType.CONTACT -> {
            ConversationVCardAttachmentAvatar(
                avatarUri = avatarUri,
                titleText = titleText,
            )
        }

        ConversationVCardAttachmentType.LOCATION -> {
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
private fun ConversationVCardAttachmentAvatar(
    avatarUri: String?,
    titleText: String?,
) {
    val displayableAvatarUri = remember(avatarUri) {
        displayableVCardAvatarUri(avatarUri = avatarUri)
    }

    val label = remember(titleText) {
        vCardAvatarLabel(titleText = titleText)
    }

    Box(
        modifier = Modifier
            .size(size = VCARD_AVATAR_SIZE)
            .clip(shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        ConversationVCardAttachmentAvatarFallback(label = label)

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
private fun ConversationVCardAttachmentAvatarFallback(
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

private fun vCardAvatarLabel(titleText: String?): String? {
    return titleText
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.first()
        ?.uppercaseChar()
        ?.toString()
}

@Composable
private fun resolveTitleText(
    titleText: String?,
    titleTextResId: Int?,
): String {
    return titleText
        ?: titleTextResId?.let { titleResId ->
            stringResource(titleResId)
        }
            .orEmpty()
}

@Composable
private fun resolveSubtitleText(
    subtitleText: String?,
    subtitleTextResId: Int?,
): String? {
    return subtitleText ?: subtitleTextResId?.let { subtitleResId ->
        stringResource(subtitleResId)
    }
}
