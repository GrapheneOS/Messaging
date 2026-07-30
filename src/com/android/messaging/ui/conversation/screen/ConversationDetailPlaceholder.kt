package com.android.messaging.ui.conversation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.common.components.ListDetailPaneSide
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.common.components.LocalListDetailPaneSide
import com.android.messaging.ui.common.components.consumeOppositePaneInsets
import com.android.messaging.ui.common.components.contentSurfaceShape
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationDetailPlaceholder(modifier: Modifier = Modifier) {
    CompositionLocalProvider(
        LocalIsListDetailPane provides true,
        LocalListDetailPaneSide provides ListDetailPaneSide.End,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .consumeOppositePaneInsets()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.contentSurfaceShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.conversation_detail_no_selection_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationDetailPlaceholderPreview() {
    MessagingPreviewTheme {
        ConversationDetailPlaceholder()
    }
}
