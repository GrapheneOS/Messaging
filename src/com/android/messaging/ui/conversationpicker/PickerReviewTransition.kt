package com.android.messaging.ui.conversationpicker

import androidx.activity.BackEventCompat
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.runtime.Stable
import com.android.messaging.ui.common.components.horizontalSlideContentTransform
import com.android.messaging.ui.common.components.predictiveBackContentTransform

@Stable
internal class PickerReviewTransition(
    isReviewing: Boolean,
) {
    val transitionState = SeekableTransitionState(isReviewing)

    private var swipeEdge: Int? = null

    fun stepContentTransform(isForward: Boolean): ContentTransform {
        val transform = when (val edge = swipeEdge) {
            null -> horizontalSlideContentTransform(isForward = isForward)
            else -> predictiveBackContentTransform(swipeEdge = edge)
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
        swipeEdge = backEvent.swipeEdge

        transitionState.seekTo(
            fraction = backEvent.progress,
            targetState = false,
        )
    }

    suspend fun settleTo(isReviewing: Boolean) {
        transitionState.animateTo(isReviewing)

        swipeEdge = null
    }
}

private const val REVEALED_CONTENT_Z_INDEX = -1.0f
