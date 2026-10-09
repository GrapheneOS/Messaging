package com.android.messaging.ui.conversation.composer.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.imeAwareBottomBarInsets
import com.android.messaging.ui.conversation.CONVERSATION_READ_ONLY_NOTICE_TEST_TAG
import com.android.messaging.ui.core.MessagingPreviewTheme

@Composable
internal fun ConversationReadOnlyNotice(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(id = R.string.conversation_cannot_reply),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(imeAwareBottomBarInsets())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(CONVERSATION_READ_ONLY_NOTICE_TEST_TAG),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@PreviewLightDark
@Composable
private fun ConversationReadOnlyNoticePreview() {
    MessagingPreviewTheme {
        ConversationReadOnlyNotice()
    }
}
