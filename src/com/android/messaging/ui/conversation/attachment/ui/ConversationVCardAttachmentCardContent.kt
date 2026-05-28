package com.android.messaging.ui.conversation.attachment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.preview.previewVCardUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.util.UriUtil

private val VCARD_AVATAR_SIZE = 36.dp
private val VCARD_AVATAR_ICON_SIZE = 20.dp
private const val PREVIEW_LOCAL_AVATAR_URI =
    "android.resource://com.android.messaging/drawable/ic_launcher_foreground"
private const val PREVIEW_LONG_CONTACT_TITLE =
    "Alexandria Cassandra Montgomery-Washington from International Partnerships"
private const val PREVIEW_LONG_CONTACT_SUBTITLE =
    "mobile +1 415 555 0198, work +1 415 555 0134, alexandria.montgomery-washington@example.com"
private const val PREVIEW_LONG_LOCATION_TITLE =
    "Northwest corner entrance near the visitor center and underground parking"
private const val PREVIEW_LONG_LOCATION_SUBTITLE =
    "1600 Amphitheatre Parkway, Building 43, Mountain View, California 94043, United States"

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

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentLoadedPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewLoadedConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
            )
            PreviewLoadedConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.LOCATION,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentContactVisualPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = "Ada Lovelace",
                titleTextResId = null,
                subtitleText = "+31 6 2222 3333",
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = PREVIEW_LOCAL_AVATAR_URI,
                titleText = "Marina Silva",
                titleTextResId = null,
                subtitleText = "marina@example.com",
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.vcard_tap_hint,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentTextStatePreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewConversationVCardAttachmentCardContent(
                modifier = Modifier.width(width = 220.dp),
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = PREVIEW_LONG_CONTACT_TITLE,
                titleTextResId = null,
                subtitleText = PREVIEW_LONG_CONTACT_SUBTITLE,
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                modifier = Modifier.width(width = 220.dp),
                type = ConversationVCardAttachmentType.LOCATION,
                avatarUri = null,
                titleText = PREVIEW_LONG_LOCATION_TITLE,
                titleTextResId = null,
                subtitleText = PREVIEW_LONG_LOCATION_SUBTITLE,
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = "Kai",
                titleTextResId = null,
                subtitleText = null,
                subtitleTextResId = null,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentMetadataStatePreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.loading_vcard,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.failed_loading_vcard,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.LOCATION,
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_location,
                subtitleText = "Shared map pin",
                subtitleTextResId = null,
            )
        }
    }
}

@Composable
private fun PreviewLoadedConversationVCardAttachmentCardContent(
    type: ConversationVCardAttachmentType,
) {
    val uiModel = previewVCardUiModel(type = type)
    PreviewConversationVCardAttachmentCardContent(
        type = uiModel.type,
        avatarUri = uiModel.avatarUri,
        titleText = uiModel.titleText,
        titleTextResId = uiModel.titleTextResId,
        subtitleText = uiModel.subtitleText,
        subtitleTextResId = uiModel.subtitleTextResId,
    )
}

@Composable
private fun PreviewConversationVCardAttachmentCardContent(
    modifier: Modifier = Modifier,
    type: ConversationVCardAttachmentType,
    avatarUri: String?,
    titleText: String?,
    titleTextResId: Int?,
    subtitleText: String?,
    subtitleTextResId: Int?,
) {
    ConversationVCardAttachmentCardContent(
        modifier = modifier,
        type = type,
        avatarUri = avatarUri,
        titleText = titleText,
        titleTextResId = titleTextResId,
        subtitleText = subtitleText,
        subtitleTextResId = subtitleTextResId,
    )
}
