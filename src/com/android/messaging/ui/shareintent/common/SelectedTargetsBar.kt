package com.android.messaging.ui.shareintent.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SelectedTargetsBar(
    targets: ImmutableList<ShareTargetUiState>,
    onRemove: (ShareTargetUiState) -> Unit,
    onSend: () -> Unit,
    showSendButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var previousCount by remember { mutableIntStateOf(targets.size) }

    LaunchedEffect(targets.size) {
        if (targets.size > previousCount) {
            listState.animateScrollToItem(targets.lastIndex)
        }
        previousCount = targets.size
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SelectedBarHeight),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = SelectedBarStartPadding,
                end = SelectedBarEndPadding,
                bottom = SelectedBarBottomPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(SelectedChipSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            items(
                items = targets,
                key = { it.key },
            ) { target ->
                SelectedTargetChip(
                    target = target,
                    onRemove = { onRemove(target) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (showSendButton) {
            SelectedTargetsSendButton(onSend = onSend)
        }
    }
}

@Composable
private fun BoxScope.SelectedTargetsSendButton(
    onSend: () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(SelectedBarEndPadding)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            ),
    )

    FilledIconButton(
        onClick = onSend,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = SelectedBarStartPadding)
            .size(SelectedSendButtonSize),
        shape = RoundedCornerShape(SelectedSendButtonCornerRadius),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.Send,
            contentDescription = stringResource(R.string.share_selection_send),
        )
    }
}

@Composable
private fun SelectedTargetChip(
    target: ShareTargetUiState,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(SelectedChipAvatarSize),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(SelectedChipAvatarSize)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
            ) {
                ParticipantAvatar(
                    avatarUri = target.avatarUri,
                    size = SelectedChipAvatarSize,
                    fallbackIconSize = FallbackIconSize,
                    fallbackIcon = when (target) {
                        is ShareTargetUiState.Conversation -> when {
                            target.isGroup -> Icons.Default.Group
                            else -> Icons.Default.Person
                        }

                        is ShareTargetUiState.Contact -> Icons.Default.Person
                    },
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(SelectedChipRemoveBadgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(
                        R.string.share_selection_remove,
                        target.displayName,
                    ),
                    modifier = Modifier.size(SelectedChipRemoveIconSize),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(SelectedChipLabelSpacing))

        Text(
            text = target.displayName,
            modifier = Modifier
                .fillMaxWidth()
                .height(SelectedChipLabelHeight),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewLightDark
@Composable
private fun SelectedTargetsBarPreview() {
    MessagingPreviewColumn {
        SelectedTargetsBar(
            targets = persistentListOf(
                ShareTargetUiState.Conversation(
                    conversationId = "1",
                    normalizedDestination = "+31612345678",
                    displayName = "Jane Doe",
                    details = "+31 6 1234 5678",
                    avatarUri = null,
                    isGroup = false,
                ),
                ShareTargetUiState.Conversation(
                    conversationId = "2",
                    normalizedDestination = null,
                    displayName = "Project group",
                    details = null,
                    avatarUri = null,
                    isGroup = true,
                ),
            ),
            onRemove = {},
            onSend = {},
            showSendButton = true,
        )
    }
}
