@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.recipientpicker.component

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.messaging.R
import com.android.messaging.ui.common.components.participant.ParticipantAvatar
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import com.android.messaging.ui.recipientselection.preview.previewSelectedRecipient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val selectedRecipientAvatarSize = 24.dp
private val selectedRecipientChipAvatarLabelSpacing = 4.dp
private val selectedRecipientChipEndPadding = 12.dp
private val selectedRecipientChipRemoveAreaWidth = 32.dp
private val selectedRecipientChipStartPadding = 4.dp
private val selectedRecipientTrailingContentKey = Any()

private const val SELECTED_RECIPIENT_CHIP_REMOVE_ICON_HIDDEN_SCALE = 0.8f
private const val SELECTED_RECIPIENT_TRAILING_CONTENT_Z_INDEX = -1f

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
        BoundsTransform { _, _ -> selectedRecipientChipAnimationSpec() }
    }
    val entranceGate = remember { SelectedRecipientChipEntranceGate() }

    SideEffect {
        entranceGate.animatesEntrances = true
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        LookaheadScope {
            FlowRow(
                // A fixed width keeps the size animation from clipping a new chip and, in RTL,
                // from moving the lookahead frame that new chips are placed in
                modifier = modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = selectedRecipientChipAnimationSpec()),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                leadingContent?.invoke()

                recipients.forEach { recipient ->
                    key(recipient.destination) {
                        val isArmed = recipient.destination == armedRecipientDestination
                        val revealProgress = rememberSelectedRecipientChipRevealProgress(
                            startsHidden = entranceGate.animatesEntrances,
                        )

                        RecipientSelectionSelectedRecipientChip(
                            modifier = Modifier.animateBounds(
                                lookaheadScope = this@LookaheadScope,
                                boundsTransform = chipBoundsTransform,
                            ),
                            recipient = recipient,
                            isArmed = isArmed,
                            enabled = enabled,
                            revealProgress = { revealProgress.value },
                            onClick = { onRecipientClick(recipient) },
                        )
                    }
                }

                trailingContent?.let { content ->
                    key(selectedRecipientTrailingContentKey) {
                        content(
                            Modifier
                                .zIndex(zIndex = SELECTED_RECIPIENT_TRAILING_CONTENT_Z_INDEX)
                                .animateBounds(
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

private class SelectedRecipientChipEntranceGate {
    var animatesEntrances: Boolean = false
}

@Composable
private fun rememberSelectedRecipientChipRevealProgress(startsHidden: Boolean): State<Float> {
    val progress = remember {
        Animatable(
            initialValue = when {
                startsHidden -> 0f
                else -> 1f
            },
        )
    }

    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = selectedRecipientChipAnimationSpec(),
        )
    }

    return progress.asState()
}

@Composable
private fun RecipientSelectionSelectedRecipientChip(
    recipient: SelectedRecipient,
    isArmed: Boolean,
    enabled: Boolean,
    revealProgress: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipContentDescription = selectedRecipientChipContentDescription(
        recipient = recipient,
        isArmed = isArmed,
    )
    val chipContainerColor = MaterialTheme.colorScheme.surfaceVariant
    val chipContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val chipShape = InputChipDefaults.shape

    Surface(
        selected = isArmed,
        onClick = onClick,
        modifier = modifier
            .revealFromStart(
                progress = revealProgress,
                shape = chipShape,
            )
            .semantics {
                contentDescription = chipContentDescription
                role = Role.Checkbox
            },
        enabled = enabled,
        shape = chipShape,
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

// A layer clip also limits hit testing, so taps on the hint over a revealing chip reach the field
private fun Modifier.revealFromStart(progress: () -> Float, shape: Shape): Modifier {
    return graphicsLayer {
        val revealedFraction = progress()
        val isRevealing = revealedFraction < 1f

        clip = isRevealing
        this.shape = when {
            isRevealing -> RevealedFromStartShape(fraction = revealedFraction, shape = shape)
            else -> RectangleShape
        }
    }
}

private class RevealedFromStartShape(
    private val fraction: Float,
    private val shape: Shape,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val revealedSize = size.copy(width = size.width * fraction)
        val revealedLeft = when (layoutDirection) {
            LayoutDirection.Ltr -> 0f
            LayoutDirection.Rtl -> size.width - revealedSize.width
        }
        val revealedOutline = shape.createOutline(
            size = revealedSize,
            layoutDirection = layoutDirection,
            density = density,
        )
        val revealedPath = Path().apply {
            addOutline(outline = revealedOutline)
            translate(offset = Offset(x = revealedLeft, y = 0f))
        }

        return Outline.Generic(path = revealedPath)
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
                    max = 1000.dp,
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
                style = MaterialTheme.typography.labelLarge.copy(
                    textDirection = TextDirection.ContentOrLtr,
                ),
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
        animationSpec = selectedRecipientChipAnimationSpec(),
        label = "SelectedRecipientChipRemoveAreaWidth",
    )

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = selectedRecipientChipAnimationSpec(),
        label = "SelectedRecipientChipRemoveIconAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = selectedRecipientChipAnimationSpec(),
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

private fun <T> selectedRecipientChipAnimationSpec(): FiniteAnimationSpec<T> {
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
    ParticipantAvatar(
        avatarUri = recipient.photoUri,
        size = selectedRecipientAvatarSize,
        fallbackLabel = participantAvatarLabel(source = recipient.label),
        modifier = modifier,
        colorSeedCode = participantColorSeed(normalizedDestination = recipient.destination),
    )
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
