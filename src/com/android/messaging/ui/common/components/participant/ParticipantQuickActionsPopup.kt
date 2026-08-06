package com.android.messaging.ui.common.components.participant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.android.messaging.R
import com.android.messaging.ui.common.components.AnchorRelativePositionProvider
import com.android.messaging.ui.common.components.MarqueeText
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val PopupWidth = 192.dp
private val ActionRowHeight = 40.dp
private val ActionRowGap = 6.dp
private val ActionRowPadding = 8.dp
private val ContentTopPadding = 4.dp
private val ContentBottomPadding = 10.dp
private val AvatarSubtitleSpacing = 10.dp
private val PopupAnchorGap = 16.dp
private val AvatarFallbackSize = 60.dp
private val IconInsidePadding = 2.dp
private val ShadowPadding = 16.dp

internal const val PARTICIPANT_QUICK_ACTIONS_COPY_AREA_TEST_TAG =
    "participant-quick-actions-copy-area"

@Composable
internal fun ParticipantQuickActionsPopup(
    visible: Boolean,
    avatarUri: String?,
    displayName: String,
    subtitle: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    onDismiss: () -> Unit,
    onMessageClick: (() -> Unit)?,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onInfoClick: (() -> Unit)?,
    colorSeedCode: String?,
    isContactSaved: Boolean = true,
    phoneNumberCopyTargets: ImmutableList<PhoneNumberCopyTarget> = persistentListOf(),
    onPhoneNumberCopy: ((String) -> Unit)? = null,
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    if (!transitionState.currentState && !transitionState.targetState) return

    val density = LocalDensity.current
    val transformOriginState = remember { mutableStateOf(TransformOrigin(0f, 1f)) }
    val positionProvider = remember(density) {
        with(density) {
            AnchorRelativePositionProvider(
                gapPx = PopupAnchorGap.roundToPx(),
                contentPaddingPx = ShadowPadding.roundToPx(),
                transformOriginState = transformOriginState,
            )
        }
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
        ),
        popupPositionProvider = positionProvider,
    ) {
        val transformOrigin = transformOriginState.value

        AnimatedVisibility(
            visibleState = transitionState,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialScale = 0.8f,
                transformOrigin = transformOrigin,
            ) + fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = scaleOut(
                animationSpec = tween(durationMillis = 120),
                targetScale = 0.8f,
                transformOrigin = transformOrigin,
            ) + fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            QuickActionsCard(
                avatarUri = avatarUri,
                displayName = displayName,
                subtitle = subtitle,
                fallbackIcon = fallbackIcon,
                fallbackLabel = fallbackLabel,
                colorSeedCode = colorSeedCode,
                onMessageClick = onMessageClick,
                onCallClick = onCallClick,
                onContactClick = onContactClick,
                onInfoClick = onInfoClick,
                isContactSaved = isContactSaved,
                phoneNumberCopyTargets = phoneNumberCopyTargets,
                onPhoneNumberCopy = onPhoneNumberCopy,
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    avatarUri: String?,
    displayName: String,
    subtitle: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    colorSeedCode: String?,
    onMessageClick: (() -> Unit)?,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onInfoClick: (() -> Unit)?,
    isContactSaved: Boolean,
    phoneNumberCopyTargets: ImmutableList<PhoneNumberCopyTarget>,
    onPhoneNumberCopy: ((String) -> Unit)?,
) {
    val cardShape = MaterialTheme.shapes.medium
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(
        modifier = Modifier
            .padding(ShadowPadding)
            .width(PopupWidth),
        shape = cardShape,
        color = cardColor,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PhoneNumberCopyArea(
                targets = phoneNumberCopyTargets,
                onPhoneNumberCopy = onPhoneNumberCopy,
            ) {
                AvatarHeader(
                    avatarUri = avatarUri,
                    fallbackIcon = fallbackIcon,
                    fallbackLabel = fallbackLabel,
                    colorSeedCode = colorSeedCode,
                    fadeColor = cardColor,
                )

                Spacer(modifier = Modifier.height(ContentTopPadding))

                MarqueeText(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fadeEdgeWidth = ActionRowPadding,
                )

                if (!subtitle.isNullOrBlank()) {
                    MarqueeText(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fadeEdgeWidth = ActionRowPadding,
                    )
                }

                Spacer(modifier = Modifier.height(AvatarSubtitleSpacing))
            }

            QuickActionsRow(
                onMessageClick = onMessageClick,
                onCallClick = onCallClick,
                onContactClick = onContactClick,
                onInfoClick = onInfoClick,
                isContactSaved = isContactSaved,
            )

            Spacer(modifier = Modifier.height(ContentBottomPadding))
        }
    }
}

@Composable
private fun PhoneNumberCopyArea(
    targets: ImmutableList<PhoneNumberCopyTarget>,
    onPhoneNumberCopy: ((String) -> Unit)?,
    content: @Composable () -> Unit,
) {
    var showTargetMenu by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val copyLabel = stringResource(R.string.copy_to_clipboard)
    val canCopy = targets.isNotEmpty() && onPhoneNumberCopy != null
    val performAction: () -> Unit = {
        when (targets.size) {
            0 -> Unit
            1 -> onPhoneNumberCopy?.invoke(targets.single().phoneNumber)
            else -> showTargetMenu = true
        }
    }
    val interactionModifier = when {
        !canCopy -> Modifier
        else -> Modifier.combinedClickable(
            role = Role.Button,
            onClickLabel = copyLabel,
            onClick = performAction,
            onLongClickLabel = copyLabel,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                performAction()
            },
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PARTICIPANT_QUICK_ACTIONS_COPY_AREA_TEST_TAG)
                .then(interactionModifier),
        ) {
            content()
        }

        DropdownMenu(
            expanded = showTargetMenu,
            onDismissRequest = { showTargetMenu = false },
        ) {
            targets.forEach { target ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = target.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (target.displayName != target.phoneNumber) {
                                Text(
                                    text = target.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        showTargetMenu = false
                        onPhoneNumberCopy?.invoke(target.phoneNumber)
                    },
                )
            }
        }
    }
}

@Composable
private fun AvatarHeader(
    avatarUri: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    colorSeedCode: String?,
    fadeColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        ParticipantAvatar(
            avatarUri = avatarUri,
            fallbackIcon = fallbackIcon,
            fallbackSize = AvatarFallbackSize,
            fallbackLabel = fallbackLabel,
            colorSeedCode = colorSeedCode,
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(0.5f to Color.Transparent, 1f to fadeColor),
                ),
        )
    }
}

@Composable
private fun QuickActionsRow(
    onMessageClick: (() -> Unit)?,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onInfoClick: (() -> Unit)?,
    isContactSaved: Boolean,
) {
    val actions = listOfNotNull(onMessageClick, onCallClick, onContactClick, onInfoClick)

    if (actions.isEmpty()) {
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ActionRowPadding),
        horizontalArrangement = Arrangement.spacedBy(ActionRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val buttonModifier = Modifier.weight(1f)

        onMessageClick?.let {
            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.Chat,
                contentDescription = stringResource(R.string.action_send_message),
                onClick = it,
                modifier = buttonModifier,
            )
        }

        onCallClick?.let {
            QuickActionButton(
                icon = Icons.Default.Call,
                contentDescription = stringResource(R.string.action_call),
                onClick = it,
                modifier = buttonModifier,
            )
        }

        onContactClick?.let {
            val icon = when {
                isContactSaved -> Icons.Default.Person
                else -> Icons.Default.PersonAdd
            }
            val labelRes = when {
                isContactSaved -> R.string.action_contact_info
                else -> R.string.action_add_contact
            }

            QuickActionButton(
                icon = icon,
                contentDescription = stringResource(labelRes),
                onClick = it,
                modifier = buttonModifier,
            )
        }

        onInfoClick?.let {
            QuickActionButton(
                icon = Icons.Default.Info,
                contentDescription = stringResource(R.string.action_people_and_options),
                onClick = it,
                modifier = buttonModifier,
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(ActionRowHeight),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(IconInsidePadding),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ParticipantQuickActionsPopupPreview() {
    MessagingPreviewColumn {
        QuickActionsCard(
            avatarUri = null,
            displayName = "This is my best friend with the longest name ever",
            subtitle = "+1 555 000 0000000 (Mobile, USA)",
            fallbackIcon = Icons.Default.Person,
            fallbackLabel = "T",
            colorSeedCode = null,
            onMessageClick = {},
            onCallClick = {},
            onContactClick = {},
            onInfoClick = {},
            isContactSaved = false,
            phoneNumberCopyTargets = persistentListOf(
                PhoneNumberCopyTarget(
                    displayName = "Best friend",
                    phoneNumber = "+1 555 000 0000000",
                ),
            ),
            onPhoneNumberCopy = {},
        )
    }
}
