package com.android.messaging.ui.conversationpicker

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import com.android.messaging.ui.common.components.PREDICTIVE_BACK_DURATION_MILLIS
import com.android.messaging.ui.common.components.horizontalSlideContentTransform
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
internal class PickerReviewTransition(
    isReviewing: Boolean,
) {
    val transitionState = SeekableTransitionState(isReviewing)

    private var swipeEdge: Int? = null
    private var cancelledSwipe: Job? = null

    fun stepContentTransform(
        isForward: Boolean,
        predictiveBackContentTransform: (swipeEdge: Int) -> ContentTransform,
    ): ContentTransform {
        val transform = when (val edge = swipeEdge) {
            null -> horizontalSlideContentTransform(isForward = isForward)
            else -> predictiveBackContentTransform(edge)
        }

        return ContentTransform(
            targetContentEnter = transform.targetContentEnter,
            initialContentExit = transform.initialContentExit,
            targetContentZIndex = when {
                isForward -> transform.targetContentZIndex
                else -> REVEALED_CONTENT_Z_INDEX
            },
            sizeTransform = null,
        )
    }

    suspend fun seekToTargets(backEvent: BackEventCompat) {
        if (swipeEdge == null) {
            swipeEdge = backEvent.swipeEdge
            // Compose delays an animation by the play time at which it first gets its target, and
            // the transform's animations only get theirs when drawn. So each swipe draws a frame at
            // 0 first, as NavDisplay's start event does, or the page's scale would trail the finger.
            transitionState.seekTo(fraction = 0f, targetState = false)
            withFrameNanos {}
        }

        transitionState.seekTo(
            fraction = backEvent.progress,
            targetState = false,
        )
    }

    /**
     * Plays a cancelled swipe back to its start, as NavDisplay does. Animating to the review state
     * instead would reverse the transition: the review page would replay its enter, and the page
     * behind it would get the predictive exit.
     */
    fun cancelSwipe(scope: CoroutineScope) {
        cancelledSwipe = scope.launch {
            val fraction = transitionState.fraction
            coroutineScope {
                animate(
                    initialValue = fraction,
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = (fraction * PREDICTIVE_BACK_DURATION_MILLIS).roundToInt(),
                    ),
                ) { value, _ ->
                    launch { transitionState.seekTo(value) }
                }
            }
            transitionState.snapTo(true)
            swipeEdge = null
            // Waits for the settled state to compose, so the next swipe gets a new transform.
            transitionState.animateTo(true)
        }
    }

    /**
     * Stops a cancelled swipe playing back, so a swipe that starts meanwhile takes the transition
     * over from wherever it is, as NavDisplay does.
     */
    suspend fun takeOverCancelledSwipe() {
        cancelledSwipe?.cancelAndJoin()
    }

    suspend fun settleTo(isReviewing: Boolean) {
        cancelledSwipe?.cancelAndJoin()
        transitionState.animateTo(isReviewing)

        swipeEdge = null
    }
}

/** Closes the review page with the back swipe, or plays the swipe back if it's cancelled. */
@Composable
internal fun PickerReviewBackHandler(
    reviewTransition: PickerReviewTransition,
    enabled: Boolean,
    onDismissed: () -> Unit,
) {
    val settleScope = rememberCoroutineScope()

    PredictiveBackHandler(enabled = enabled) { progress ->
        reviewTransition.takeOverCancelledSwipe()
        try {
            progress.collect { backEvent ->
                reviewTransition.seekToTargets(backEvent = backEvent)
            }
            onDismissed()
        } catch (cancellation: CancellationException) {
            reviewTransition.cancelSwipe(scope = settleScope)
            throw cancellation
        }
    }
}

private const val REVEALED_CONTENT_Z_INDEX = -1.0f
