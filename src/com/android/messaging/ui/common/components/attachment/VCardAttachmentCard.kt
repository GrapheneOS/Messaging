package com.android.messaging.ui.common.components.attachment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
    avatarName: String?,
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
            avatarName = avatarName,
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
    avatarName: String?,
) {
    when (kind) {
        VCardAttachmentKind.Contact -> {
            VCardAvatar(
                avatarUri = avatarUri,
                avatarName = avatarName,
                size = VCARD_AVATAR_SIZE,
                iconSize = VCARD_AVATAR_ICON_SIZE,
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
