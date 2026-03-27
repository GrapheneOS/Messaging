package com.android.messaging.ui.conversation.v2.metadata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.metadata.model.ConversationMetadataUiState

private val CONVERSATION_TOP_APP_BAR_TITLE_SPACING = 12.dp
private val CONVERSATION_TOP_APP_BAR_AVATAR_SIZE = 36.dp
private val CONVERSATION_TOP_APP_BAR_AVATAR_ICON_SIZE = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationTopAppBar(
    modifier: Modifier = Modifier,
    metadata: ConversationMetadataUiState,
    onNavigateBack: () -> Unit,
) {
    val presentation = rememberConversationTopAppBarPresentation(
        metadata = metadata,
    )

    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = conversationTopAppBarColors(),
        title = {
            ConversationTopAppBarTitle(
                presentation = presentation,
            )
        },
        navigationIcon = {
            ConversationTopAppBarNavigationIcon(
                onNavigateBack = onNavigateBack,
            )
        },
    )
}

@Composable
private fun conversationTopAppBarColors(): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun rememberConversationTopAppBarPresentation(
    metadata: ConversationMetadataUiState,
): ConversationTopAppBarPresentation {
    val title = conversationTitle(
        metadata = metadata,
    )
    val subtitle = conversationSubtitle(
        metadata = metadata,
    )
    val isGroupConversation = conversationIsGroup(
        metadata = metadata,
    )

    return remember(
        metadata,
        title,
        subtitle,
        isGroupConversation,
    ) {
        ConversationTopAppBarPresentation(
            title = title,
            subtitle = subtitle,
            isGroupConversation = isGroupConversation,
        )
    }
}

@Composable
private fun ConversationTopAppBarTitle(
    presentation: ConversationTopAppBarPresentation,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = CONVERSATION_TOP_APP_BAR_TITLE_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationAvatar(
            isGroupConversation = presentation.isGroupConversation,
        )

        ConversationTopAppBarText(
            presentation = presentation,
        )
    }
}

@Composable
private fun ConversationTopAppBarText(
    presentation: ConversationTopAppBarPresentation,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = presentation.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (presentation.subtitle != null) {
            Text(
                text = presentation.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConversationTopAppBarNavigationIcon(
    onNavigateBack: () -> Unit,
) {
    IconButton(
        onClick = onNavigateBack,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(id = R.string.back),
        )
    }
}

@Composable
private fun ConversationAvatar(
    isGroupConversation: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(size = CONVERSATION_TOP_APP_BAR_AVATAR_SIZE),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    isGroupConversation -> Icons.Rounded.Group
                    else -> Icons.Rounded.Person
                },
                contentDescription = null,
                modifier = Modifier.size(size = CONVERSATION_TOP_APP_BAR_AVATAR_ICON_SIZE),
            )
        }
    }
}

@Composable
private fun conversationTitle(
    metadata: ConversationMetadataUiState,
): String {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> stringResource(id = R.string.app_name)
        ConversationMetadataUiState.Unavailable -> stringResource(id = R.string.app_name)

        is ConversationMetadataUiState.Present -> {
            metadata
                .title
                .takeIf { it.isNotBlank() }
                ?: stringResource(id = R.string.app_name)
        }
    }
}

private fun conversationIsGroup(
    metadata: ConversationMetadataUiState,
): Boolean {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> false
        ConversationMetadataUiState.Unavailable -> false
        is ConversationMetadataUiState.Present -> metadata.isGroupConversation
    }
}

@Composable
private fun conversationSubtitle(
    metadata: ConversationMetadataUiState,
): String? {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> stringResource(id = R.string.loading_messages)
        ConversationMetadataUiState.Unavailable -> null

        is ConversationMetadataUiState.Present -> {
            when {
                metadata.isGroupConversation && metadata.participantCount > 1 -> {
                    pluralStringResource(
                        id = R.plurals.wearable_participant_count,
                        count = metadata.participantCount,
                        metadata.participantCount,
                    )
                }

                else -> null
            }
        }
    }
}

@Immutable
private data class ConversationTopAppBarPresentation(
    val title: String,
    val subtitle: String?,
    val isGroupConversation: Boolean,
)
