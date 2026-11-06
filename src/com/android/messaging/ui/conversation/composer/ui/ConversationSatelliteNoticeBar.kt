package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_SATELLITE_NOTICE_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSatelliteNotice
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationSatelliteNoticeBar(
    notice: ConversationSatelliteNotice,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(CONVERSATION_SATELLITE_NOTICE_TEST_TAG),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.SatelliteAlt,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(id = notice.messageResId()),
            modifier = Modifier.weight(weight = 1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun ConversationSatelliteNotice.messageResId(): Int {
    return when (this) {
        ConversationSatelliteNotice.TextAndMultimedia -> {
            R.string.conversation_satellite_notice_text_and_multimedia
        }

        ConversationSatelliteNotice.TextOnly -> R.string.conversation_satellite_notice_text_only

        ConversationSatelliteNotice.MessagingUnavailable -> {
            R.string.conversation_satellite_notice_messaging_unavailable
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationSatelliteNoticeBarPreview() {
    MessagingPreviewTheme {
        ConversationSatelliteNoticeBar(notice = ConversationSatelliteNotice.TextOnly)
    }
}
