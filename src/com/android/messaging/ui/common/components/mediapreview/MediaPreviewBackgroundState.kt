package com.android.messaging.ui.common.components.mediapreview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val MEDIA_PREVIEW_BACKGROUND_CROSSFADE_MILLIS = 180

internal fun resolveMediaPreviewBackgroundBlendPages(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int,
): MediaPreviewBackgroundBlendPages {
    if (pageCount <= 0) {
        return MediaPreviewBackgroundBlendPages(
            lowerPage = 0,
            upperPage = 0,
        )
    }

    val lastPage = pageCount - 1
    val clampedCurrentPage = currentPage.coerceIn(
        minimumValue = 0,
        maximumValue = lastPage,
    )

    val unclampedLowerPage = when {
        currentPageOffsetFraction < 0f -> clampedCurrentPage - 1
        else -> clampedCurrentPage
    }
    val lowerPage = unclampedLowerPage.coerceIn(
        minimumValue = 0,
        maximumValue = lastPage,
    )

    return MediaPreviewBackgroundBlendPages(
        lowerPage = lowerPage,
        upperPage = (lowerPage + 1).coerceAtMost(maximumValue = lastPage),
    )
}

internal fun resolveMediaPreviewBackgroundUpperAlpha(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    lowerPage: Int,
    pageCount: Int,
): Float {
    if (pageCount <= 1) {
        return 0f
    }

    val continuousPage = (currentPage + currentPageOffsetFraction).coerceIn(
        minimumValue = 0f,
        maximumValue = (pageCount - 1).toFloat(),
    )
    return (continuousPage - lowerPage).coerceIn(
        minimumValue = 0f,
        maximumValue = 1f,
    )
}

internal fun resolveMediaPreviewBackgroundInteractivePairReady(
    currentPageOffsetFraction: Float,
    blendPages: MediaPreviewBackgroundBlendPages,
    hasLowerFrame: Boolean,
    hasUpperFrame: Boolean,
): Boolean {
    val isUpperFrameRequired = blendPages.lowerPage != blendPages.upperPage &&
        currentPageOffsetFraction != 0f

    return hasLowerFrame && (!isUpperFrameRequired || hasUpperFrame)
}

internal fun getMediaPreviewBackgroundPrefetchItems(
    items: ImmutableList<MediaPreviewItem>,
    currentPage: Int,
    settledPage: Int,
    targetPage: Int,
    blendPages: MediaPreviewBackgroundBlendPages,
): ImmutableList<MediaPreviewItem> {
    if (items.isEmpty()) {
        return persistentListOf()
    }

    val candidatePages = listOf(
        settledPage,
        targetPage,
        currentPage,
        blendPages.lowerPage,
        blendPages.upperPage,
        targetPage - 1,
        targetPage + 1,
        currentPage - 1,
        currentPage + 1,
        settledPage - 1,
        settledPage + 1,
    )
    val seenContentUris = mutableSetOf<String>()

    return candidatePages
        .asSequence()
        .mapNotNull(items::getOrNull)
        .filter { item -> seenContentUris.add(item.contentUri) }
        .toImmutableList()
}

internal fun mediaPreviewBackgroundFrame(
    items: ImmutableList<MediaPreviewItem>,
    page: Int,
    bitmapCache: MediaPreviewBitmapCache,
): MediaPreviewBackgroundFrame? {
    return items
        .getOrNull(index = page)
        ?.let { item ->
            bitmapCache[item.contentUri]?.let { imageBitmap ->
                MediaPreviewBackgroundFrame(
                    contentUri = item.contentUri,
                    imageBitmap = imageBitmap,
                )
            }
        }
}

internal fun resolveMediaPreviewBackgroundRenderState(
    isScrollInProgress: Boolean,
    frames: MediaPreviewBackgroundFrames,
    transitionState: MediaPreviewBackgroundTransitionState,
): MediaPreviewBackgroundRenderState {
    val incomingFrame = transitionState.incomingFrame
    val displayedFrame = transitionState.displayedFrame
    val interactiveLowerFrame = frames.lowerFrame

    val canRenderInteractiveState = isScrollInProgress &&
        !transitionState.isScrollFallbackLatched &&
        frames.isInteractivePairReady &&
        interactiveLowerFrame != null

    return when {
        incomingFrame != null -> {
            MediaPreviewBackgroundRenderState.Recovering(
                displayedFrame = displayedFrame,
                incomingFrame = incomingFrame,
                recoveryProgress = transitionState.recoveryProgress,
            )
        }

        canRenderInteractiveState -> {
            MediaPreviewBackgroundRenderState.Interactive(
                lowerFrame = interactiveLowerFrame,
                upperFrame = frames.upperFrame,
                lowerPage = frames.blendPages.lowerPage,
            )
        }

        displayedFrame != null -> {
            MediaPreviewBackgroundRenderState.Held(
                frame = displayedFrame,
            )
        }

        else -> MediaPreviewBackgroundRenderState.Fallback
    }
}

@Stable
internal class MediaPreviewBackgroundTransitionState(
    private val recoveryAnimator: suspend (Animatable<Float, AnimationVector1D>) -> Unit =
        ::animateMediaPreviewBackgroundRecovery,
) {
    val recoveryProgress = Animatable(initialValue = 0f)

    var displayedFrame by mutableStateOf<MediaPreviewBackgroundFrame?>(value = null)
        private set

    var incomingFrame by mutableStateOf<MediaPreviewBackgroundFrame?>(value = null)
        private set

    var isScrollFallbackLatched by mutableStateOf(value = false)
        private set

    private var scrollSession = 0
    private var isScrollSessionActive = false

    suspend fun clear() {
        recoveryProgress.snapTo(targetValue = 0f)
        displayedFrame = null
        incomingFrame = null
        isScrollFallbackLatched = false
        isScrollSessionActive = false
    }

    fun onScrollFrame(
        isInteractivePairReady: Boolean,
        currentPageFrame: MediaPreviewBackgroundFrame?,
    ) {
        if (!isScrollSessionActive) {
            scrollSession += 1
            isScrollSessionActive = true
            isScrollFallbackLatched = incomingFrame != null
        }

        val shouldLatchScrollFallback = !isInteractivePairReady
        val shouldDisplayCurrentPageFrame = currentPageFrame != null &&
            !isScrollFallbackLatched &&
            incomingFrame == null

        when {
            shouldLatchScrollFallback -> {
                isScrollFallbackLatched = true
            }

            shouldDisplayCurrentPageFrame -> {
                displayedFrame = currentPageFrame
            }
        }
    }

    suspend fun settle(frame: MediaPreviewBackgroundFrame) {
        val canSettleScrollSession = isScrollSessionActive &&
            !isScrollFallbackLatched &&
            incomingFrame == null

        if (canSettleScrollSession) {
            displayedFrame = frame
            finishScrollSession(scrollSessionToFinish = scrollSession)
            return
        }

        recoverTo(frame = frame)
    }

    private suspend fun recoverTo(frame: MediaPreviewBackgroundFrame) {
        val recoveryScrollSession = scrollSession

        if (displayedFrame?.contentUri == frame.contentUri) {
            finishScrollSession(scrollSessionToFinish = recoveryScrollSession)
            return
        }

        recoveryProgress.snapTo(targetValue = 0f)
        incomingFrame = frame
        recoveryAnimator(recoveryProgress)

        displayedFrame = frame
        incomingFrame = null
        finishScrollSession(scrollSessionToFinish = recoveryScrollSession)
    }

    private fun finishScrollSession(scrollSessionToFinish: Int) {
        if (scrollSessionToFinish == scrollSession) {
            isScrollFallbackLatched = false
            isScrollSessionActive = false
        }
    }
}

private suspend fun animateMediaPreviewBackgroundRecovery(
    recoveryProgress: Animatable<Float, AnimationVector1D>,
) {
    recoveryProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = MEDIA_PREVIEW_BACKGROUND_CROSSFADE_MILLIS,
            easing = LinearEasing,
        ),
    )
}

@Immutable
internal data class MediaPreviewBackgroundBlendPages(
    val lowerPage: Int,
    val upperPage: Int,
)

@Immutable
internal data class MediaPreviewBackgroundFrame(
    val contentUri: String,
    val imageBitmap: ImageBitmap,
)

@Immutable
internal data class MediaPreviewBackgroundFrames(
    val bitmapCache: MediaPreviewBitmapCache,
    val blendPages: MediaPreviewBackgroundBlendPages,
    val lowerFrame: MediaPreviewBackgroundFrame?,
    val upperFrame: MediaPreviewBackgroundFrame?,
    val currentPageFrame: MediaPreviewBackgroundFrame?,
    val isInteractivePairReady: Boolean,
)

@Immutable
internal sealed interface MediaPreviewBackgroundRenderState {
    data object Fallback : MediaPreviewBackgroundRenderState

    @Immutable
    data class Held(
        val frame: MediaPreviewBackgroundFrame,
    ) : MediaPreviewBackgroundRenderState

    @Immutable
    data class Interactive(
        val lowerFrame: MediaPreviewBackgroundFrame,
        val upperFrame: MediaPreviewBackgroundFrame?,
        val lowerPage: Int,
    ) : MediaPreviewBackgroundRenderState

    @Immutable
    data class Recovering(
        val displayedFrame: MediaPreviewBackgroundFrame?,
        val incomingFrame: MediaPreviewBackgroundFrame,
        val recoveryProgress: Animatable<Float, AnimationVector1D>,
    ) : MediaPreviewBackgroundRenderState
}
