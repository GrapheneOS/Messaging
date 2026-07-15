package com.android.messaging.ui.conversation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

private val BLOCKED_BANNER_MARGIN = 12.dp
private val BLOCKED_BANNER_ICON_CONTAINER_SIZE = 48.dp
private val BLOCKED_BANNER_ICON_SIZE = 32.dp

@Composable
internal fun rememberBlockedBannerRevealState(
    conversationId: String?,
    isBlocked: Boolean,
    isContentLoaded: Boolean,
): Boolean {
    var isRevealed by remember(conversationId) { mutableStateOf(false) }

    LaunchedEffect(conversationId, isBlocked, isContentLoaded) {
        if (isBlocked && isContentLoaded) {
            delay(1.seconds)
            isRevealed = true
        } else {
            isRevealed = false
        }
    }

    return isRevealed
}

@Composable
internal fun ConversationBlockedBannerSlot(
    isRevealed: Boolean,
    onUnblockClick: () -> Unit,
    onHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    AnimatedVisibility(
        modifier = modifier.fillMaxWidth(),
        visible = isRevealed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        ConversationBlockedBanner(
            modifier = Modifier
                .padding(BLOCKED_BANNER_MARGIN)
                .onSizeChanged { size ->
                    with(density) {
                        onHeightChanged(size.height.toDp())
                    }
                },
            onUnblockClick = onUnblockClick,
        )
    }
}

@Composable
private fun ConversationBlockedBanner(
    modifier: Modifier,
    onUnblockClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ConversationBlockedBannerIcon()

                Column(
                    modifier = Modifier.weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.conversation_blocked_banner_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = stringResource(id = R.string.conversation_blocked_banner_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TextButton(
                modifier = Modifier.align(alignment = Alignment.End),
                onClick = onUnblockClick,
            ) {
                Text(
                    text = stringResource(id = R.string.conversation_blocked_banner_action),
                )
            }
        }
    }
}

@Composable
private fun ConversationBlockedBannerIcon() {
    Box(
        modifier = Modifier
            .size(BLOCKED_BANNER_ICON_CONTAINER_SIZE)
            .clip(shape = CircleShape)
            .background(color = MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            modifier = Modifier.size(BLOCKED_BANNER_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationBlockedBannerPreview() {
    MessagingPreviewTheme {
        ConversationBlockedBanner(
            modifier = Modifier.padding(BLOCKED_BANNER_MARGIN),
            onUnblockClick = {},
        )
    }
}
