package com.android.messaging.ui.common.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.SwipeEdge
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * The part of a predictive back that follows the swipe: the closing page shrinks towards the side
 * the swipe moves it to. The transition is seeked to the swipe's progress, so it can't tell a
 * release from a hold; what plays once the swipe commits is [predictiveBackPage]'s.
 */
@Composable
internal fun rememberPredictiveBackContentTransform(): (swipeEdge: Int) -> ContentTransform {
    val edgeMarginPx = with(LocalDensity.current) { EDGE_MARGIN.roundToPx() }

    return remember(edgeMarginPx) {
        { swipeEdge ->
            val shrink = scaleOut(
                animationSpec = predictiveBackGestureSpec(),
                targetScale = PREDICTIVE_BACK_TARGET_SCALE,
            )
            val push = slideOutHorizontally(animationSpec = predictiveBackGestureSpec()) { width ->
                predictiveBackTranslation(
                    swipeEdge = swipeEdge,
                    fullWidth = width,
                    edgeMarginPx = edgeMarginPx,
                )
            }

            EnterTransition.None togetherWith shrink + push
        }
    }
}

/**
 * Right-edge swipes shrink the screen in place; the others push it right until its right edge is
 * [edgeMarginPx] from the screen edge.
 */
internal fun predictiveBackTranslation(
    @SwipeEdge swipeEdge: Int,
    fullWidth: Int,
    edgeMarginPx: Int,
): Int {
    if (swipeEdge == NavigationEvent.EDGE_RIGHT) {
        return 0
    }

    val slack = fullWidth * (1f - PREDICTIVE_BACK_TARGET_SCALE) / 2f

    return (slack - edgeMarginPx).roundToInt().coerceAtLeast(0)
}

internal fun <T> predictiveBackGestureSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
        easing = PredictiveBackGestureEasing,
    )
}

/**
 * Plays what [rememberPredictiveBackContentTransform]'s transition can't: the closing page follows
 * the finger up and down, and only once the swipe commits does it fade and the revealed page slide
 * into place as its scrim lifts. Holding the swipe at its end, as holding Back with three-button
 * navigation does, keeps both pages where they are, and cancelling it plays the transition back.
 *
 * [isOpen] is whether this is the page the user is on. A commit takes the closing page off and
 * puts the revealed one on, so that's how each tells a commit from a cancel.
 */
@Composable
internal fun Modifier.predictiveBackPage(
    transition: Transition<EnterExitState>,
    isOpen: Boolean,
): Modifier {
    val gestureState = LocalNavigationEventDispatcherOwner
        .current
        ?.navigationEventDispatcher
        ?.transitionState

    val density = LocalDensity.current
    val edgeMarginPx = with(density) { EDGE_MARGIN.toPx() }
    val revealOffsetPx = with(density) { REVEAL_OFFSET.toPx() }

    val scrimAlpha = when {
        isSystemInDarkTheme() -> DARK_SCRIM_ALPHA
        else -> LIGHT_SCRIM_ALPHA
    }

    val role = rememberPageRole(transition = transition, gestureState = gestureState)
    val progress = transition.animateClosingProgress(role = role)
    val releaseProgress = rememberReleaseProgress(
        role = role,
        isOpen = isOpen,
        progress = progress,
    )

    val settle = rememberSettle(role = role, isOpen = isOpen)
    val finger = rememberPredictiveBackFinger(gestureState = gestureState, isOpen = isOpen)

    return this
        .graphicsLayer {
            translationX = -revealOffsetPx * (1f - settle.value)
            if (role == PageRole.Closing) {
                val scale = lerp(
                    start = 1f,
                    stop = PREDICTIVE_BACK_TARGET_SCALE,
                    fraction = PredictiveBackGestureEasing.transform(progress.value),
                )
                val shift = predictiveBackVerticalShift(
                    touchDeltaY = finger.touchDeltaY,
                    scale = scale,
                    height = size.height,
                    edgeMarginPx = edgeMarginPx,
                )
                translationY = shift / scale
                alpha = predictiveBackReleaseAlpha(
                    progress = progress.value,
                    releaseProgress = releaseProgress.floatValue,
                )
            }
        }
        .drawWithContent {
            drawContent()
            val scrim = scrimAlpha * (1f - settle.value)
            if (scrim > 0f) {
                drawRect(color = Color.Black, alpha = scrim)
            }
        }
}

/**
 * A swipe gives the transition its target before it seeks it, so the swipe is still in progress
 * then, and a commit or cancel doesn't retarget it. Only the latest gesture state matters.
 */
@Composable
private fun rememberPageRole(
    transition: Transition<EnterExitState>,
    gestureState: StateFlow<NavigationEventTransitionState>?,
): PageRole {
    return remember(transition.targetState) {
        when {
            gestureState?.value !is InProgress -> PageRole.Other
            transition.targetState == EnterExitState.PostExit -> PageRole.Closing
            transition.currentState == EnterExitState.PreEnter -> PageRole.Revealed
            else -> PageRole.Other
        }
    }
}

@Composable
private fun Transition<EnterExitState>.animateClosingProgress(role: PageRole): State<Float> {
    return animateFloat(
        transitionSpec = {
            when (role) {
                PageRole.Closing -> tween(
                    durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                    easing = LinearEasing,
                )
                else -> snap()
            }
        },
        label = "PredictiveBackProgress",
    ) { state ->
        when {
            state == EnterExitState.PostExit && role == PageRole.Closing -> 1f
            else -> 0f
        }
    }
}

/**
 * Where a commit took the closing page off, or NaN until one does
 */
@Composable
private fun rememberReleaseProgress(
    role: PageRole,
    isOpen: Boolean,
    progress: State<Float>,
): FloatState {
    val releaseProgress = remember(role) { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(releaseProgress, isOpen) {
        if (role == PageRole.Closing && !isOpen) {
            releaseProgress.floatValue = progress.value
        }
    }

    return releaseProgress
}

/**
 * Settles the revealed page once a commit puts it on. Not the transition's: that ends as soon as a
 * swipe held at its end is released.
 */
@Composable
private fun rememberSettle(role: PageRole, isOpen: Boolean): State<Float> {
    val currentIsOpen by rememberUpdatedState(isOpen)
    val settle = remember { Animatable(if (role == PageRole.Revealed) 0f else 1f) }
    LaunchedEffect(settle) {
        if (settle.value < 1f) {
            snapshotFlow { currentIsOpen }.first { it }
            settle.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = PREDICTIVE_BACK_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    return settle.asState()
}

@Composable
private fun rememberPredictiveBackFinger(
    gestureState: StateFlow<NavigationEventTransitionState>?,
    isOpen: Boolean,
): PredictiveBackFinger {
    val currentIsOpen by rememberUpdatedState(isOpen)
    val finger = remember { PredictiveBackFinger() }
    LaunchedEffect(finger, gestureState) {
        gestureState?.collect { state -> finger.track(state = state, isOpen = currentIsOpen) }
    }

    return finger
}

/**
 * The closing page fades over what's left of the transition once the swipe commits at
 * [releaseProgress]; NaN while it hasn't.
 */
internal fun predictiveBackReleaseAlpha(progress: Float, releaseProgress: Float): Float {
    return when {
        releaseProgress.isNaN() || progress <= releaseProgress -> 1f
        else -> {
            1f - FastOutSlowInEasing.transform(
                (progress - releaseProgress) / (1f - releaseProgress),
            )
        }
    }
}

private enum class PageRole {
    Closing,
    Revealed,
    Other,
}

/**
 * Follows the latest gesture state only, so a cancel and the next swipe's start that both land
 * before it looks count as one swipe, as they do for NavDisplay.
 */
private class PredictiveBackFinger {
    private var startTouchY = Float.NaN

    var touchDeltaY by mutableFloatStateOf(0f)
        private set

    /** Only the open page follows the finger, so a committed page keeps its shift as it leaves. */
    fun track(state: NavigationEventTransitionState, isOpen: Boolean) {
        val event = (state as? InProgress)?.latestEvent
        when {
            event == null -> startTouchY = Float.NaN
            !isOpen -> Unit
            startTouchY.isNaN() -> {
                startTouchY = event.touchY
                touchDeltaY = 0f
            }
            else -> touchDeltaY = event.touchY - startTouchY
        }
    }
}

internal fun predictiveBackVerticalShift(
    touchDeltaY: Float,
    scale: Float,
    height: Float,
    edgeMarginPx: Float,
): Float {
    val room = height * (1f - scale) / 2f - edgeMarginPx
    if (room <= 0f) {
        return 0f
    }

    val travel = (abs(touchDeltaY) / (height / 2f)).coerceAtMost(1f)
    val deceleratedTravel = 1f - (1f - travel) * (1f - travel)

    return room * deceleratedTravel * sign(touchDeltaY)
}

private val PredictiveBackGestureEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

internal const val PREDICTIVE_BACK_TARGET_SCALE = 0.85f
internal const val PREDICTIVE_BACK_DURATION_MILLIS = 450
private const val DARK_SCRIM_ALPHA = 0.8f
private const val LIGHT_SCRIM_ALPHA = 0.2f
private val EDGE_MARGIN = 8.dp
private val REVEAL_OFFSET = 96.dp
