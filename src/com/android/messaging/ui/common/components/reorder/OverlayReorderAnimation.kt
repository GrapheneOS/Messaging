package com.android.messaging.ui.common.components.reorder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val REORDER_OVERLAY_DURATION_MILLIS = 320

private const val REORDER_OVERLAY_Z_INDEX = 10f

private const val OFFSCREEN_FADE_DURATION_MILLIS = 120

@Stable
internal class OverlayReorderAnimationController<T, K : Any>(
    private val key: (T) -> K,
    private val isSettled: (item: T, anchorToTop: Boolean) -> Boolean,
) {

    private val geometry = OverlayReorderGeometry()

    private var containerBounds = Rect.Zero
    private var contentTopInRoot = 0f
    private var itemsByKey: Map<K, T> = emptyMap()
    private var itemIndexByKey: Map<K, Int> = emptyMap()
    private val itemBoundsByKey = mutableMapOf<K, Rect>()
    private var nextAnimationId = 0L

    internal var animations by mutableStateOf<List<OverlayReorderState<T>>>(emptyList())
        private set

    fun updateContainerBounds(bounds: Rect) {
        containerBounds = bounds
    }

    fun updateContentTop(topInRoot: Float) {
        contentTopInRoot = topInRoot
    }

    fun updateItems(items: List<T>) {
        itemsByKey = items.associateBy(key)
        itemIndexByKey = items
            .mapIndexed { index, item ->
                key(item) to index
            }
            .toMap()
    }

    fun updateItemBounds(
        itemKey: K,
        boundsInRoot: Rect,
        isPhysicallyVisible: Boolean,
        firstVisibleItemIndex: Int,
        lastVisibleItemIndex: Int,
    ) {
        val localBounds = boundsInRoot.translate(
            Offset(
                x = -containerBounds.left,
                y = -containerBounds.top,
            ),
        )
        itemBoundsByKey[itemKey] = localBounds

        val animationIndex = animations.indexOfFirst { animation -> animation.key == itemKey }
        val animation = animations.getOrNull(animationIndex) ?: return
        val targetIndex = itemIndexByKey[itemKey] ?: return
        val isLogicallyVisible = targetIndex in firstVisibleItemIndex..lastVisibleItemIndex
        val isModelSettled = itemsByKey[itemKey]?.let { item ->
            isSettled(item, animation.anchorToTop)
        } == true

        val isAcceptable = geometry.isAcceptableTarget(
            isCommitted = animation.isCommitted,
            isStarted = animation.isStarted,
            sourceIndex = animation.sourceIndex,
            sourceTop = animation.sourceBounds.top,
            candidateTop = localBounds.top,
            targetIndex = targetIndex,
            isPhysicallyVisible = isPhysicallyVisible,
            isLogicallyVisible = isLogicallyVisible,
            isModelSettled = isModelSettled,
        )

        if (isAcceptable) {
            updateAnimationAt(animationIndex) { state ->
                state.copy(targetBounds = localBounds)
            }
        }
    }

    fun removeItemBounds(itemKey: K) {
        itemBoundsByKey.remove(itemKey)
    }

    fun prepare(
        keys: Collection<K>,
        anchorToTop: Boolean,
        transform: (T) -> T,
    ) {
        animations = keys.mapNotNull { itemKey ->
            val item = itemsByKey[itemKey] ?: return@mapNotNull null
            val sourceBounds = itemBoundsByKey[itemKey] ?: return@mapNotNull null
            val sourceIndex = itemIndexByKey[itemKey] ?: return@mapNotNull null

            OverlayReorderState(
                animationId = nextAnimationId++,
                key = itemKey,
                item = transform(item),
                sourceBounds = sourceBounds,
                sourceIndex = sourceIndex,
                targetBounds = null,
                anchorToTop = anchorToTop,
                isCommitted = false,
                isStarted = false,
            )
        }
    }

    fun markCommitted() {
        animations = animations.map { animation ->
            animation.copy(isCommitted = true)
        }
    }

    fun isItemHidden(itemKey: K): Boolean {
        return animations.any { animation ->
            animation.key == itemKey
        }
    }

    fun startAnimation(animationId: Long): OverlayReorderState<T>? {
        val animationIndex = animations.indexOfFirst { animation ->
            animation.animationId == animationId
        }

        if (animationIndex < 0) {
            return null
        }

        updateAnimationAt(animationIndex) { state ->
            state.copy(isStarted = true)
        }

        return animations[animationIndex]
    }

    fun fallbackTarget(animation: OverlayReorderState<T>): Rect {
        return geometry.fallbackTarget(
            sourceBounds = animation.sourceBounds,
            anchorToTop = animation.anchorToTop,
            contentTopInRoot = contentTopInRoot,
            containerBounds = containerBounds,
        )
    }

    fun finish(animationId: Long) {
        animations = animations.filterNot { animation ->
            animation.animationId == animationId
        }
    }

    private fun updateAnimationAt(
        index: Int,
        transform: (OverlayReorderState<T>) -> OverlayReorderState<T>,
    ) {
        animations = animations.toMutableList().apply {
            this[index] = transform(this[index])
        }
    }
}

internal data class OverlayReorderState<T>(
    val animationId: Long,
    val key: Any,
    val item: T,
    val sourceBounds: Rect,
    val sourceIndex: Int,
    val targetBounds: Rect?,
    val anchorToTop: Boolean,
    val isCommitted: Boolean,
    val isStarted: Boolean,
)

@Composable
internal fun <T, K : Any> rememberOverlayReorderAnimationController(
    key: (T) -> K,
    isSettled: (item: T, anchorToTop: Boolean) -> Boolean,
): OverlayReorderAnimationController<T, K> {
    return remember { OverlayReorderAnimationController(key, isSettled) }
}

@Composable
internal fun <T> OverlayReorderAnimation(
    controller: OverlayReorderAnimationController<T, *>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        controller.animations.forEach { animation ->
            key(animation.animationId) {
                OverlayReorderItem(
                    animation = animation,
                    controller = controller,
                    itemContent = itemContent,
                )
            }
        }
    }
}

@Composable
private fun <T> OverlayReorderItem(
    animation: OverlayReorderState<T>,
    controller: OverlayReorderAnimationController<T, *>,
    itemContent: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current
    val alpha = remember(animation.animationId) { Animatable(1f) }
    val scale = remember(animation.animationId) { Animatable(1.05f) }
    val position = remember(animation.animationId) {
        Animatable(animation.sourceBounds.topLeft, Offset.VectorConverter)
    }

    LaunchedEffect(
        animation.animationId,
        animation.isCommitted,
    ) {
        runOverlayReorderAnimation(
            controller = controller,
            animation = animation,
            position = position,
            alpha = alpha,
            scale = scale,
        )
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = position.value.x.roundToInt(),
                    y = position.value.y.roundToInt(),
                )
            }
            .width(with(density) { animation.sourceBounds.width.toDp() })
            .height(with(density) { animation.sourceBounds.height.toDp() })
            .zIndex(REORDER_OVERLAY_Z_INDEX)
            .graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
                shadowElevation = 8.dp.toPx()
            },
    ) {
        itemContent(animation.item)
    }
}

private suspend fun <T> runOverlayReorderAnimation(
    controller: OverlayReorderAnimationController<T, *>,
    animation: OverlayReorderState<T>,
    position: Animatable<Offset, AnimationVector2D>,
    alpha: Animatable<Float, AnimationVector1D>,
    scale: Animatable<Float, AnimationVector1D>,
) {
    if (!animation.isCommitted) {
        return
    }

    withFrameNanos { }

    val latestAnimation = controller.startAnimation(animation.animationId) ?: return
    val targetBounds = latestAnimation.targetBounds ?: controller.fallbackTarget(latestAnimation)
    val usesFallbackTarget = latestAnimation.targetBounds == null

    val isAtTargetHorizontally =
        abs(targetBounds.left - animation.sourceBounds.left) <= TARGET_POSITION_EPSILON_PX
    val isAtTargetVertically =
        abs(targetBounds.top - animation.sourceBounds.top) <= TARGET_POSITION_EPSILON_PX
    val isAlreadyAtTarget = isAtTargetHorizontally && isAtTargetVertically

    if (!isAlreadyAtTarget) {
        coroutineScope {
            launch {
                position.animateTo(
                    targetValue = targetBounds.topLeft,
                    animationSpec = tween(
                        durationMillis = REORDER_OVERLAY_DURATION_MILLIS,
                        easing = when {
                            usesFallbackTarget -> FastOutLinearInEasing
                            else -> FastOutSlowInEasing
                        },
                    ),
                )
            }

            if (usesFallbackTarget) {
                launch {
                    alpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = OFFSCREEN_FADE_DURATION_MILLIS,
                            delayMillis = REORDER_OVERLAY_DURATION_MILLIS -
                                OFFSCREEN_FADE_DURATION_MILLIS,
                        ),
                    )
                }
            }

            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = REORDER_OVERLAY_DURATION_MILLIS,
                    ),
                )
            }
        }
    }

    controller.finish(animation.animationId)
}
