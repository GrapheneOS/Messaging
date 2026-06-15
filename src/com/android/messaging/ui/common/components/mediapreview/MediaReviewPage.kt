package com.android.messaging.ui.common.components.mediapreview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

private const val MEDIA_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS = 160
private const val MEDIA_REVIEW_DELETE_CHIP_ANIMATION_DURATION_MILLIS = 120
private const val MEDIA_REVIEW_DELETE_CHIP_BACKGROUND_ALPHA = 0.5f
private const val MEDIA_REVIEW_PAGE_MIN_SCALE = 0.98f
private const val MEDIA_REVIEW_REMOVAL_MIN_SCALE = 0.9f
private val MediaReviewDeleteChipSize = 32.dp
private val MediaReviewDeleteChipIconSize = 18.dp

@Immutable
internal data class MediaReviewRemovalState(
    val isInteractionEnabled: Boolean,
    val isDeleteChipVisible: Boolean,
    val deleteChipVisibilityProgress: Float,
    val removalProgress: Float,
    val markPendingRemoval: () -> Unit,
)

@Composable
internal fun rememberMediaReviewRemovalState(
    itemKey: String,
    isOnlyItem: Boolean,
    shouldShowDeleteChip: Boolean,
    onRemove: () -> Unit,
    onClearAfterLastRemoval: () -> Unit,
): MediaReviewRemovalState {
    var isPendingRemoval by remember(itemKey) {
        mutableStateOf(value = false)
    }

    val deleteChipVisibilityProgress by animateFloatAsState(
        targetValue = when {
            shouldShowDeleteChip && !isPendingRemoval -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = MEDIA_REVIEW_DELETE_CHIP_ANIMATION_DURATION_MILLIS),
        label = "mediaReviewDeleteChipVisibility",
    )

    val removalProgress by animateFloatAsState(
        targetValue = when {
            isPendingRemoval -> 0f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = MEDIA_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS,
        ),
        label = "mediaReviewPageRemovalVisibility",
    )

    LaunchedEffect(isPendingRemoval) {
        if (!isPendingRemoval) {
            return@LaunchedEffect
        }

        delay(timeMillis = MEDIA_REVIEW_PAGE_REMOVE_ANIMATION_DURATION_MILLIS.toLong())
        onRemove()

        if (isOnlyItem) {
            onClearAfterLastRemoval()
        }
    }

    return MediaReviewRemovalState(
        isInteractionEnabled = !isPendingRemoval,
        isDeleteChipVisible = deleteChipVisibilityProgress > 0f,
        deleteChipVisibilityProgress = deleteChipVisibilityProgress,
        removalProgress = removalProgress,
        markPendingRemoval = {
            if (!isPendingRemoval) {
                isPendingRemoval = true
            }
        },
    )
}

internal fun Modifier.mediaReviewPageTransform(
    page: Int,
    pagerState: PagerState,
    removalProgress: Float,
): Modifier {
    return graphicsLayer {
        val pageOffset = resolveMediaReviewPageOffset(
            page = page,
            pagerState = pagerState,
        )
        val pageScale = lerp(
            start = MEDIA_REVIEW_PAGE_MIN_SCALE,
            stop = 1f,
            fraction = 1f - pageOffset,
        )
        val removalScale = lerp(
            start = MEDIA_REVIEW_REMOVAL_MIN_SCALE,
            stop = 1f,
            fraction = removalProgress,
        )
        alpha = removalProgress
        scaleX = pageScale * removalScale
        scaleY = pageScale * removalScale
    }
}

internal fun mediaReviewVisibleDeleteChipPage(
    pagerState: PagerState,
    pageCount: Int,
): Int? {
    if (pageCount <= 0) {
        return null
    }

    val lastIndex = pageCount - 1
    val clampedCurrentPage = pagerState.currentPage.coerceIn(
        minimumValue = 0,
        maximumValue = lastIndex,
    )
    val clampedSettledPage = pagerState.settledPage.coerceIn(
        minimumValue = 0,
        maximumValue = lastIndex,
    )

    return when {
        !pagerState.isScrollInProgress -> clampedCurrentPage
        clampedCurrentPage == clampedSettledPage -> null
        else -> clampedCurrentPage
    }
}

@Composable
internal fun MediaReviewDeleteChip(
    visibilityProgress: Float,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = lerp(
        start = MEDIA_REVIEW_REMOVAL_MIN_SCALE,
        stop = 1f,
        fraction = visibilityProgress,
    )

    FilledIconButton(
        modifier = modifier
            .size(size = MediaReviewDeleteChipSize)
            .graphicsLayer {
                alpha = visibilityProgress
                scaleX = scale
                scaleY = scale
            },
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color.Black.copy(alpha = MEDIA_REVIEW_DELETE_CHIP_BACKGROUND_ALPHA),
            contentColor = Color.White,
        ),
    ) {
        Icon(
            modifier = Modifier.size(size = MediaReviewDeleteChipIconSize),
            imageVector = Icons.Rounded.Close,
            contentDescription = contentDescription,
        )
    }
}

private fun resolveMediaReviewPageOffset(
    page: Int,
    pagerState: PagerState,
): Float {
    val rawPageOffset = when {
        pagerState.isScrollInProgress -> {
            pagerState.currentPage - page + pagerState.currentPageOffsetFraction
        }

        else -> (pagerState.settledPage - page).toFloat()
    }

    return rawPageOffset.absoluteValue.coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}
