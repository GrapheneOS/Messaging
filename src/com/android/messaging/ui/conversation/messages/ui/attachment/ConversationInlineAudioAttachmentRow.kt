package com.android.messaging.ui.conversation.messages.ui.attachment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment

private val AUDIO_ATTACHMENT_HEIGHT = 70.dp

@Composable
internal fun ConversationInlineAudioAttachmentRow(
    attachment: ConversationInlineAttachment.Audio,
    isIncoming: Boolean,
    isSelectionMode: Boolean,
    useStandaloneAudioAttachmentBackground: Boolean,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val contentUri = attachment.contentUri

    val title = attachment.titleText
        ?: attachment.titleTextResId?.let { stringResource(it) }
        ?: stringResource(R.string.audio_attachment_content_description)

    val colors = rememberConversationInlineAudioAttachmentColors(
        isIncoming = isIncoming,
        isSelectionMode = isSelectionMode,
        useStandaloneAudioAttachmentBackground = useStandaloneAudioAttachmentBackground,
    )

    val playbackState = rememberConversationInlineAudioAttachmentPlaybackState(
        contentUri = contentUri,
    )

    ConversationInlineAudioAttachmentRowContent(
        colors = colors,
        isSelectionMode = isSelectionMode,
        isPlaying = playbackState.isPlaying,
        title = title,
        durationLabel = playbackState.durationLabel,
        progress = playbackState.progress,
        onClick = {
            playbackState.togglePlayback(
                context = context,
                contentUri = contentUri,
            )
        },
        onLongClick = onLongClick,
    )
}

@Composable
internal fun ConversationInlineAudioAttachmentRowContent(
    colors: ConversationInlineAudioAttachmentColors,
    isSelectionMode: Boolean,
    isPlaying: Boolean,
    title: String,
    durationLabel: String,
    progress: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val modifier = when {
        isSelectionMode -> Modifier

        else -> {
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = AUDIO_ATTACHMENT_HEIGHT)
            .then(modifier),
        color = colors.container,
        shape = RoundedCornerShape(size = MESSAGE_ATTACHMENT_CORNER_RADIUS),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConversationInlineAudioAttachmentPlayButton(
                isPlaying = isPlaying,
                colors = colors,
            )

            ConversationInlineAudioAttachmentContent(
                title = title,
                durationLabel = durationLabel,
                isPlaying = isPlaying,
                progress = progress,
                colors = colors,
            )
        }
    }
}

@Composable
private fun ConversationInlineAudioAttachmentPlayButton(
    isPlaying: Boolean,
    colors: ConversationInlineAudioAttachmentColors,
) {
    Surface(
        modifier = Modifier
            .size(size = 40.dp)
            .testTag(tag = CONVERSATION_INLINE_AUDIO_ATTACHMENT_PLAY_BUTTON_TEST_TAG),
        color = colors.playButton,
        shape = RoundedCornerShape(size = 20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    isPlaying -> Icons.Rounded.Pause
                    else -> Icons.Rounded.PlayArrow
                },
                contentDescription = when {
                    isPlaying -> stringResource(R.string.audio_pause_content_description)
                    else -> stringResource(R.string.audio_play_content_description)
                },
                tint = colors.playIcon,
            )
        }
    }
}

@Composable
private fun RowScope.ConversationInlineAudioAttachmentContent(
    title: String,
    durationLabel: String,
    isPlaying: Boolean,
    progress: Float,
    colors: ConversationInlineAudioAttachmentColors,
) {
    val shouldShowProgress = isPlaying || progress > 0f

    Column(
        modifier = Modifier
            .weight(weight = 1f)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(weight = 1f, fill = false),
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.content,
                maxLines = 1,
            )

            Text(
                modifier = Modifier
                    .width(width = 48.dp),
                text = durationLabel,
                style = MaterialTheme.typography.labelMedium,
                color = colors.secondaryContent,
            )
        }

        AnimatedVisibility(
            visible = shouldShowProgress,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(tag = CONVERSATION_INLINE_AUDIO_ATTACHMENT_PROGRESS_TEST_TAG),
                progress = { progress },
                color = colors.progress,
                drawStopIndicator = {},
                strokeCap = StrokeCap.Butt,
                trackColor = colors.progressTrack,
            )
        }
    }
}

@Composable
internal fun rememberConversationInlineAudioAttachmentColors(
    isIncoming: Boolean,
    isSelectionMode: Boolean,
    useStandaloneAudioAttachmentBackground: Boolean,
): ConversationInlineAudioAttachmentColors {
    return ConversationInlineAudioAttachmentColors(
        container = getAudioAttachmentContainerColor(
            isIncoming = isIncoming,
            useStandaloneAudioAttachmentBackground = useStandaloneAudioAttachmentBackground,
        ),
        content = getAudioAttachmentContentColor(isIncoming = isIncoming),
        playButton = getAudioAttachmentPlayButtonColor(
            isIncoming = isIncoming,
            isSelectionMode = isSelectionMode,
        ),
        playIcon = getAudioAttachmentPlayIconColor(
            isIncoming = isIncoming,
            isSelectionMode = isSelectionMode,
        ),
        progress = getAudioAttachmentProgressColor(isIncoming = isIncoming),
        progressTrack = getAudioAttachmentProgressTrackColor(isIncoming = isIncoming),
        secondaryContent = getAudioAttachmentSecondaryContentColor(isIncoming = isIncoming),
    )
}


@Composable
private fun getAudioAttachmentContainerColor(
    isIncoming: Boolean,
    useStandaloneAudioAttachmentBackground: Boolean,
): Color {
    return when {
        !useStandaloneAudioAttachmentBackground -> {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }

        isIncoming -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

@Composable
private fun getAudioAttachmentContentColor(
    isIncoming: Boolean,
): Color {
    return when {
        isIncoming -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
}

@Composable
private fun getAudioAttachmentPlayButtonColor(
    isIncoming: Boolean,
    isSelectionMode: Boolean,
): Color {
    return when {
        isSelectionMode -> MaterialTheme.colorScheme.surfaceVariant
        isIncoming -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun getAudioAttachmentPlayIconColor(
    isIncoming: Boolean,
    isSelectionMode: Boolean,
): Color {
    return when {
        isSelectionMode -> MaterialTheme.colorScheme.onSurfaceVariant
        isIncoming -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimary
    }
}

@Composable
private fun getAudioAttachmentProgressColor(
    isIncoming: Boolean,
): Color {
    return when {
        isIncoming -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
}

@Composable
private fun getAudioAttachmentProgressTrackColor(
    isIncoming: Boolean,
): Color {
    return when {
        isIncoming -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    }
}

@Composable
private fun getAudioAttachmentSecondaryContentColor(
    isIncoming: Boolean,
): Color {
    return when {
        isIncoming -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    }
}

internal data class ConversationInlineAudioAttachmentColors(
    val container: Color,
    val content: Color,
    val playButton: Color,
    val playIcon: Color,
    val progress: Color,
    val progressTrack: Color,
    val secondaryContent: Color,
)
