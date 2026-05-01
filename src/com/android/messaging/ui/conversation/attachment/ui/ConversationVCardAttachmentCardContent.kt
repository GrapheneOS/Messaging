package com.android.messaging.ui.conversation.attachment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType

@Composable
internal fun ConversationVCardAttachmentCardContent(
    modifier: Modifier = Modifier,
    type: ConversationVCardAttachmentType,
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
        Box(
            modifier = Modifier.size(size = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (type) {
                    ConversationVCardAttachmentType.CONTACT -> Icons.Rounded.Person
                    ConversationVCardAttachmentType.LOCATION -> Icons.Rounded.Place
                },
                contentDescription = null,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            subtitle?.let { subtitleText ->
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
