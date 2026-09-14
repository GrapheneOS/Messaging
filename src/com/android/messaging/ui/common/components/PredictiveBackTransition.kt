package com.android.messaging.ui.common.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigationevent.NavigationEvent

internal fun predictiveBackContentTransform(swipeEdge: Int): ContentTransform {
    return predictiveBackEnter() togetherWith predictiveBackExit(swipeEdge = swipeEdge)
}

private fun predictiveBackEnter(): EnterTransition {
    return fadeIn(animationSpec = predictiveBackSpec())
}

private fun predictiveBackExit(swipeEdge: Int): ExitTransition {
    return scaleOut(
        animationSpec = predictiveBackSpec(),
        targetScale = PREDICTIVE_BACK_TARGET_SCALE,
        transformOrigin = predictiveBackTransformOrigin(swipeEdge = swipeEdge),
    )
}

private fun predictiveBackTransformOrigin(swipeEdge: Int): TransformOrigin {
    val pivotFractionX = when (swipeEdge) {
        NavigationEvent.EDGE_LEFT -> TRAILING_PIVOT_FRACTION
        NavigationEvent.EDGE_RIGHT -> LEADING_PIVOT_FRACTION
        else -> CENTER_PIVOT_FRACTION
    }

    return TransformOrigin(
        pivotFractionX = pivotFractionX,
        pivotFractionY = CENTER_PIVOT_FRACTION,
    )
}

private fun <T> predictiveBackSpec(): FiniteAnimationSpec<T> {
    return spring(
        dampingRatio = PREDICTIVE_BACK_DAMPING_RATIO,
        stiffness = PREDICTIVE_BACK_STIFFNESS,
    )
}

private const val PREDICTIVE_BACK_TARGET_SCALE = 0.9f
private const val PREDICTIVE_BACK_DAMPING_RATIO = 1.0f
private const val PREDICTIVE_BACK_STIFFNESS = 1600.0f
private const val LEADING_PIVOT_FRACTION = 0f
private const val CENTER_PIVOT_FRACTION = 0.5f
private const val TRAILING_PIVOT_FRACTION = 1f
