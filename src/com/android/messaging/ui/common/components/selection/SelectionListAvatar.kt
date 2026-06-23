package com.android.messaging.ui.common.components.selection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.android.messaging.ui.common.components.participant.ParticipantAvatar

@Composable
internal fun SelectionListAvatar(
    avatarUri: String?,
    fallbackLabel: String?,
    colorSeedCode: String?,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    fallbackIcon: ImageVector = Icons.Default.Person,
    size: Dp = SelectionListItemTokens.avatarSize,
) {
    val avatarScale by rememberSelectionListAvatarScale(isSelected = isSelected)

    AnimatedContent(
        targetState = isSelected,
        transitionSpec = {
            val enterTransition = fadeIn(
                animationSpec = tween(durationMillis = 200),
            ) + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialScale = 0.8f,
            )

            val exitTransition = fadeOut(
                animationSpec = tween(durationMillis = 150),
            ) + scaleOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                targetScale = 0.8f,
            )

            enterTransition.togetherWith(exitTransition)
        },
        label = "selectionListAvatar",
    ) { isSelectedState ->
        ParticipantAvatar(
            avatarUri = avatarUri,
            size = size,
            fallbackLabel = fallbackLabel,
            fallbackSize = SelectionListItemTokens.avatarFallbackSize,
            fallbackIcon = fallbackIcon,
            modifier = modifier.graphicsLayer {
                scaleX = avatarScale
                scaleY = avatarScale
            },
            colorSeedCode = colorSeedCode,
            isSelected = isSelectedState,
        )
    }
}

@Composable
private fun rememberSelectionListAvatarScale(
    isSelected: Boolean,
): State<Float> {
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "selectionListAvatarScale",
    )

    return selectionTransition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "selectionListAvatarScaleValue",
        targetValueByState = { isAvatarSelected ->
            when {
                isAvatarSelected -> 1f
                else -> 0.9f
            }
        },
    )
}
