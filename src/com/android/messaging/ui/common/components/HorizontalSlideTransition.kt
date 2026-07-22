package com.android.messaging.ui.common.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

internal fun horizontalSlideContentTransform(isForward: Boolean): ContentTransform {
    return when {
        isForward -> slideInFromRight() togetherWith slideOutToLeft()
        else -> slideInFromLeft() togetherWith slideOutToRight()
    }
}

internal fun slideInFromRight(): EnterTransition {
    val slide = slideInHorizontally(animationSpec = slideSpec()) { fullWidth -> fullWidth }
    return slide + fadeIn(animationSpec = fadeSpec())
}

internal fun slideOutToLeft(): ExitTransition {
    val slide = slideOutHorizontally(animationSpec = slideSpec()) { fullWidth ->
        -fullWidth / SLIDE_PARALLAX_DIVISOR
    }
    return slide + fadeOut(animationSpec = fadeSpec())
}

internal fun slideInFromLeft(): EnterTransition {
    val slide = slideInHorizontally(animationSpec = slideSpec()) { fullWidth ->
        -fullWidth / SLIDE_PARALLAX_DIVISOR
    }
    return slide + fadeIn(animationSpec = fadeSpec())
}

internal fun slideOutToRight(): ExitTransition {
    val slide = slideOutHorizontally(animationSpec = slideSpec()) { fullWidth -> fullWidth }
    return slide + fadeOut(animationSpec = fadeSpec())
}

private fun slideSpec(): FiniteAnimationSpec<IntOffset> {
    return tween(durationMillis = TRANSITION_DURATION_MILLIS)
}

private fun fadeSpec(): FiniteAnimationSpec<Float> {
    return tween(durationMillis = TRANSITION_DURATION_MILLIS)
}

private const val SLIDE_PARALLAX_DIVISOR = 4
private const val TRANSITION_DURATION_MILLIS = 350
