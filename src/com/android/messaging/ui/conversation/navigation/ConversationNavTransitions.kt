package com.android.messaging.ui.conversation.navigation

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
import androidx.navigation3.ui.NavDisplay

internal fun messageDetailsTransitionMetadata(): Map<String, Any> {
    return NavDisplay.transitionSpec {
        slideInFromRight() togetherWith slideOutToLeft()
    } + NavDisplay.popTransitionSpec {
        slideInFromLeft() togetherWith slideOutToRight()
    } + NavDisplay.predictivePopTransitionSpec { _ ->
        slideInFromLeft() togetherWith slideOutToRight()
    }
}

private fun slideInFromRight(): EnterTransition {
    val slide = slideInHorizontally(animationSpec = slideSpec()) { fullWidth -> fullWidth }
    return slide + fadeIn(animationSpec = fadeSpec())
}

private fun slideOutToLeft(): ExitTransition {
    val slide = slideOutHorizontally(animationSpec = slideSpec()) { fullWidth ->
        -fullWidth / SLIDE_PARALLAX_DIVISOR
    }
    return slide + fadeOut(animationSpec = fadeSpec())
}

private fun slideInFromLeft(): EnterTransition {
    val slide = slideInHorizontally(animationSpec = slideSpec()) { fullWidth ->
        -fullWidth / SLIDE_PARALLAX_DIVISOR
    }
    return slide + fadeIn(animationSpec = fadeSpec())
}

private fun slideOutToRight(): ExitTransition {
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
