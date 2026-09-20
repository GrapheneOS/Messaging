package com.android.messaging.ui.common.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.SwipeEdge
import kotlin.math.roundToInt

internal fun predictiveBackContentTransform(
    @SwipeEdge swipeEdge: Int,
): ContentTransform {
    return EnterTransition.None togetherWith predictiveBackExit(swipeEdge = swipeEdge)
}

private fun predictiveBackExit(
    @SwipeEdge swipeEdge: Int,
): ExitTransition {
    val shrink = scaleOut(
        animationSpec = predictiveBackSpec(),
        targetScale = PREDICTIVE_BACK_TARGET_SCALE,
    )
    val push = slideOutHorizontally(animationSpec = predictiveBackSpec()) { fullWidth ->
        predictiveBackTranslation(swipeEdge = swipeEdge, fullWidth = fullWidth)
    }

    return shrink + push
}

internal fun predictiveBackTranslation(
    @SwipeEdge swipeEdge: Int,
    fullWidth: Int,
): Int {
    val slack = fullWidth * (1f - PREDICTIVE_BACK_TARGET_SCALE) / 2f
    val travel = (slack - fullWidth * PREDICTIVE_BACK_EDGE_MARGIN_FRACTION).roundToInt()

    return when (swipeEdge) {
        NavigationEvent.EDGE_LEFT -> travel
        NavigationEvent.EDGE_RIGHT -> -travel
        else -> 0
    }
}

internal fun <T> predictiveBackSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
        easing = LinearEasing,
    )
}

internal const val PREDICTIVE_BACK_TARGET_SCALE = 0.75f
internal const val PREDICTIVE_BACK_EDGE_MARGIN_FRACTION = 0.025f

private const val PREDICTIVE_BACK_DURATION_MILLIS = 100
