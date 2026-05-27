@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.ui.conversation.preview.previewSelectedRecipient
import com.android.messaging.ui.conversation.recipientpicker.model.picker.SelectedRecipient
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val selectedRecipientAvatarSize = 24.dp
private val selectedRecipientChipAvatarLabelSpacing = 4.dp
private val selectedRecipientChipEndPadding = 12.dp
private val selectedRecipientChipRemoveAreaWidth = 32.dp
private val selectedRecipientChipStartPadding = 4.dp
private val selectedRecipientTrailingContentKey = Any()

private const val SELECTED_RECIPIENT_CHIP_REMOVE_ICON_HIDDEN_SCALE = 0.8f

@Composable
internal fun RecipientSelectionSelectedRecipientChips(
    recipients: ImmutableList<SelectedRecipient>,
    armedRecipientDestination: String?,
    enabled: Boolean,
    onRecipientClick: (SelectedRecipient) -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable (Modifier) -> Unit)? = null,
) {
    val chipBoundsTransform = remember {
        BoundsTransform { _, _ ->
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        }
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        LookaheadScope {
            FlowRow(
                modifier = modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                leadingContent?.invoke()

                recipients.forEach { recipient ->
                    key(recipient.destination) {
                        val isArmed = recipient.destination == armedRecipientDestination

                        RecipientSelectionSelectedRecipientChip(
                            modifier = Modifier.animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                boundsTransform = chipBoundsTransform,
                            ),
                            recipient = recipient,
                            isArmed = isArmed,
                            enabled = enabled,
                            onClick = { onRecipientClick(recipient) },
                        )
                    }
                }

                trailingContent?.let { content ->
                    key(selectedRecipientTrailingContentKey) {
                        content(
                            Modifier.animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                boundsTransform = chipBoundsTransform,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipientSelectionSelectedRecipientChip(
    recipient: SelectedRecipient,
    isArmed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipContentDescription = selectedRecipientChipContentDescription(
        recipient = recipient,
        isArmed = isArmed,
    )
    val chipContainerColor = MaterialTheme.colorScheme.surfaceVariant
    val chipContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        selected = isArmed,
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription = chipContentDescription
            role = Role.Checkbox
        },
        enabled = enabled,
        shape = InputChipDefaults.shape,
        color = chipContainerColor,
        contentColor = chipContentColor,
        border = null,
    ) {
        SelectedRecipientChipContent(
            recipient = recipient,
            isArmed = isArmed,
        )
    }
}

@Composable
private fun SelectedRecipientChipContent(
    recipient: SelectedRecipient,
    isArmed: Boolean,
) {
    BoxWithConstraints {
        val availableChipMaxWidth = maxWidth

        Row(
            modifier = Modifier
                .widthIn(
                    // Backstop when ancestor measurement passes maxWidth = Infinity
                    max = 1000.dp
                )
                .defaultMinSize(minHeight = InputChipDefaults.Height)
                .padding(
                    start = selectedRecipientChipStartPadding,
                    end = selectedRecipientChipEndPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectedRecipientChipAvatar(recipient = recipient)

            Spacer(modifier = Modifier.width(width = selectedRecipientChipAvatarLabelSpacing))

            Text(
                modifier = Modifier.widthIn(
                    max = selectedRecipientChipLabelMaxWidth(
                        maxChipWidth = availableChipMaxWidth,
                        isArmed = isArmed,
                    ),
                ),
                text = recipient.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            SelectedRecipientChipRemoveArea(isVisible = isArmed)
        }
    }
}

@Composable
private fun SelectedRecipientChipRemoveArea(
    isVisible: Boolean,
) {
    val targetWidth = when {
        isVisible -> selectedRecipientChipRemoveAreaWidth
        else -> 0.dp
    }

    val targetAlpha = when {
        isVisible -> 1f
        else -> 0f
    }

    val targetScale = when {
        isVisible -> 1f
        else -> SELECTED_RECIPIENT_CHIP_REMOVE_ICON_HIDDEN_SCALE
    }

    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = selectedRecipientChipRemoveAnimationSpec(),
        label = "SelectedRecipientChipRemoveAreaWidth",
    )

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = selectedRecipientChipRemoveAnimationSpec(),
        label = "SelectedRecipientChipRemoveIconAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = selectedRecipientChipRemoveAnimationSpec(),
        label = "SelectedRecipientChipRemoveIconScale",
    )

    Box(
        modifier = Modifier.width(width = width),
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (isVisible || width > 0.dp) {
            SelectedRecipientChipRemoveIcon(
                modifier = Modifier
                    .size(size = 24.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
    }
}

private fun <T> selectedRecipientChipRemoveAnimationSpec(): FiniteAnimationSpec<T> {
    return spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

private fun selectedRecipientChipLabelMaxWidth(
    maxChipWidth: Dp,
    isArmed: Boolean,
): Dp {
    val trailingAreaWidth = when {
        isArmed -> selectedRecipientChipRemoveAreaWidth
        else -> 0.dp
    }

    val fixedContentWidth = selectedRecipientChipStartPadding +
        selectedRecipientChipEndPadding +
        selectedRecipientAvatarSize +
        selectedRecipientChipAvatarLabelSpacing +
        trailingAreaWidth

    return (maxChipWidth - fixedContentWidth).coerceAtLeast(minimumValue = 0.dp)
}

@Composable
private fun SelectedRecipientChipAvatar(
    recipient: SelectedRecipient,
    modifier: Modifier = Modifier,
) {
    val photoUri = recipient.photoUri

    when {
        photoUri.isNullOrBlank() -> {
            SelectedRecipientTextAvatar(
                recipient = recipient,
                modifier = modifier,
            )
        }

        else -> {
            AsyncImage(
                modifier = modifier
                    .size(size = selectedRecipientAvatarSize)
                    .clip(shape = CircleShape),
                model = photoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun SelectedRecipientTextAvatar(
    recipient: SelectedRecipient,
    modifier: Modifier = Modifier,
) {
    val avatarLabel = remember(recipient.label, recipient.displayDestination) {
        recipientSelectionAvatarLabel(
            displayName = recipient.label,
            destination = recipient.displayDestination,
        )
    }

    Box(
        modifier = modifier
            .size(size = selectedRecipientAvatarSize)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun SelectedRecipientChipRemoveIcon(
    modifier: Modifier = Modifier,
) {
    Icon(
        modifier = modifier,
        imageVector = Icons.Rounded.Close,
        contentDescription = null,
    )
}

@Composable
private fun selectedRecipientChipContentDescription(
    recipient: SelectedRecipient,
    isArmed: Boolean,
): String {
    return when {
        isArmed -> {
            stringResource(
                id = R.string.recipient_selection_remove_selected_recipient_content_description,
                recipient.label,
            )
        }

        else -> {
            stringResource(
                id = R.string.recipient_selection_selected_recipient_content_description,
                recipient.label,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RecipientSelectionSelectedRecipientChipsPreview() {
    MessagingPreviewColumn {
        RecipientSelectionSelectedRecipientChips(
            recipients = persistentListOf(
                previewSelectedRecipient(),
                SelectedRecipient(
                    destination = "+37255550101",
                    label = "Grace Hopper",
                    displayDestination = "+372 5555 0101",
                    photoUri = null,
                ),
            ),
            armedRecipientDestination = "+31622223333",
            enabled = true,
            onRecipientClick = { _ -> },
        )
    }
}
