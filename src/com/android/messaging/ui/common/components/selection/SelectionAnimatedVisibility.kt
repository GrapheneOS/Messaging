package com.android.messaging.ui.common.components.selection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SelectionAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = selectionFadeScaleIn(),
    exit: ExitTransition = selectionFadeScaleOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = enter,
        exit = exit,
        content = content,
    )
}

internal fun selectionFadeScaleIn(
    initialScale: Float = 0.8f,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = 200),
    ) + scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        initialScale = initialScale,
    )
}

internal fun selectionFadeScaleOut(
    targetScale: Float = 0.8f,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = 150),
    ) + scaleOut(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        targetScale = targetScale,
    )
}
