package com.android.messaging.ui.conversationlist.common.item

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversationlist.common.support.AppearanceAnimationToken
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val SwipeBackgroundShape = RoundedCornerShape(percent = 50)

private val SwipeBackgroundOuterHorizontalPadding = 8.dp

private val SwipeBackgroundHorizontalPadding = 24.dp

private val SwipeActionThreshold = 88.dp

private val SwipeFlingMinDistance = 48.dp

private const val SWIPE_FLING_VELOCITY_THRESHOLD = 1_000f

private const val SWIPE_HORIZONTAL_VELOCITY_BIAS = 1.5f

private const val SWIPE_DIRECTION_BIAS = 1.5f

private const val SWIPE_SETTLE_VISIBILITY_THRESHOLD = 1f

private const val ARCHIVE_SLIDE_DURATION_MILLIS = 180

private const val ITEM_COLLAPSE_DURATION_MILLIS = 220

private const val ITEM_APPEARANCE_DURATION_MILLIS = 240

private val SwipeSettleSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = SWIPE_SETTLE_VISIBILITY_THRESHOLD,
)

private val SwipeArchiveSlideSpec = tween<Float>(durationMillis = ARCHIVE_SLIDE_DURATION_MILLIS)

private val ItemCollapseSpec = tween<Float>(durationMillis = ITEM_COLLAPSE_DURATION_MILLIS)

private val ItemAppearanceSpec = tween<Float>(durationMillis = ITEM_APPEARANCE_DURATION_MILLIS)

internal enum class ConversationSwipeBackground {
    Archive,
    Unarchive,
    ToggleRead,
}
private enum class SwipeDirection {
    StartToEnd,
    EndToStart,
    None,
}

private val ConversationSwipeBackground.isDismiss: Boolean
    get() = this == ConversationSwipeBackground.Archive ||
        this == ConversationSwipeBackground.Unarchive

internal data class ConversationSwipeAction(
    val background: ConversationSwipeBackground,
    val onTrigger: () -> Unit,
)

private data class SwipeBackgroundRender(
    val background: ConversationSwipeBackground,
    val alignment: Alignment,
)

@Composable
internal fun SwipeableConversationListItem(
    item: ConversationListItemUiModel,
    isSelectionMode: Boolean,
    isInteractionEnabled: Boolean,
    appearanceAnimationToken: AppearanceAnimationToken?,
    onAppearanceAnimationFinished: () -> Unit,
    startToEndAction: ConversationSwipeAction?,
    endToStartAction: ConversationSwipeAction?,
    backgroundHorizontalInsets: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentStartToEndAction by rememberUpdatedState(startToEndAction)
    val currentEndToStartAction by rememberUpdatedState(endToStartAction)

    val offsetX = remember { mutableFloatStateOf(0f) }
    val backgroundRender by remember {
        derivedStateOf {
            resolveBackgroundRender(
                offset = offsetX.floatValue,
                startToEndAction = currentStartToEndAction,
                endToStartAction = currentEndToStartAction,
            )
        }
    }
    val visibilityFraction = rememberAppearanceVisibility(
        conversationId = item.conversationId,
        appearanceAnimationToken = appearanceAnimationToken,
        onAppearanceAnimationFinished = onAppearanceAnimationFinished,
    )

    val gestureModifier = when {
        !isInteractionEnabled || isSelectionMode -> Modifier

        else -> Modifier.swipeActions(
            offsetX = offsetX,
            visibilityFraction = visibilityFraction,
            startToEndAction = { currentStartToEndAction },
            endToStartAction = { currentEndToStartAction },
        )
    }
    Box(
        modifier = modifier
            .then(gestureModifier)
            .interactionBlocking(isInteractionEnabled)
            .collapseVertically { visibilityFraction.value }
            .graphicsLayer { alpha = visibilityFraction.value },
    ) {
        ConversationListSwipeBackground(
            render = backgroundRender,
            isUnread = item.isUnread,
            modifier = Modifier
                .matchParentSize()
                .padding(backgroundHorizontalInsets)
                .padding(horizontal = SwipeBackgroundOuterHorizontalPadding),
        )

        Box(
            modifier = Modifier.offset {
                IntOffset(
                    x = offsetX.floatValue.roundToInt(),
                    y = 0,
                )
            },
        ) {
            content()
        }
    }
}

@Composable
private fun rememberAppearanceVisibility(
    conversationId: String,
    appearanceAnimationToken: AppearanceAnimationToken?,
    onAppearanceAnimationFinished: () -> Unit,
): Animatable<Float, AnimationVector1D> {
    val currentOnAppearanceAnimationFinished by rememberUpdatedState(onAppearanceAnimationFinished)
    val visibilityFraction = remember(conversationId) {
        val initialValue = when {
            appearanceAnimationToken != null -> 0f
            else -> 1f
        }

        Animatable(initialValue)
    }

    LaunchedEffect(conversationId, appearanceAnimationToken) {
        if (appearanceAnimationToken == null) {
            return@LaunchedEffect
        }

        visibilityFraction.stop()
        visibilityFraction.snapTo(0f)
        visibilityFraction.animateTo(
            targetValue = 1f,
            animationSpec = ItemAppearanceSpec,
        )
        currentOnAppearanceAnimationFinished()
    }

    return visibilityFraction
}

private fun Modifier.consumeAllPointerInput(): Modifier {
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                event.changes.forEach { change -> change.consume() }
            }
        }
    }
}

private fun Modifier.collapseVertically(fraction: () -> Float): Modifier {
    return clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * fraction().coerceIn(0f, 1f)).roundToInt()

        layout(placeable.width, height) {
            placeable.place(0, 0)
        }
    }
}

private fun Modifier.swipeActions(
    offsetX: MutableFloatState,
    visibilityFraction: Animatable<Float, AnimationVector1D>,
    startToEndAction: () -> ConversationSwipeAction?,
    endToStartAction: () -> ConversationSwipeAction?,
): Modifier {
    return pointerInput(Unit) {
        val thresholdPx = SwipeActionThreshold.toPx()
        val minFlingDistancePx = SwipeFlingMinDistance.toPx()
        val velocityTracker = VelocityTracker()

        coroutineScope {
            var settleJob: Job? = null

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                velocityTracker.resetTracking()
                settleJob?.cancel()

                var initialOverSlop = 0f
                val slopChange = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                    if (isHorizontalDrag(overSlop)) {
                        change.consume()
                        initialOverSlop = overSlop.x
                    }
                }

                if (slopChange == null) {
                    return@awaitEachGesture
                }

                velocityTracker.addPosition(
                    timeMillis = slopChange.uptimeMillis,
                    position = slopChange.position,
                )
                offsetX.floatValue += initialOverSlop

                val completed = awaitHorizontalDragToEnd(
                    pointerId = slopChange.id,
                    offsetX = offsetX,
                    velocityTracker = velocityTracker,
                )

                settleJob = when {
                    completed -> launch {
                        val velocity = velocityTracker.calculateVelocity()
                        settleSwipe(
                            offsetX = offsetX,
                            visibilityFraction = visibilityFraction,
                            thresholdPx = thresholdPx,
                            minFlingDistancePx = minFlingDistancePx,
                            width = size.width.toFloat(),
                            velocityX = velocity.x,
                            velocityY = velocity.y,
                            startToEndAction = startToEndAction(),
                            endToStartAction = endToStartAction(),
                        )
                    }

                    else -> launch {
                        animateOffset(
                            offsetX = offsetX,
                            targetValue = 0f,
                            animationSpec = SwipeSettleSpec,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitHorizontalDragToEnd(
    pointerId: PointerId,
    offsetX: MutableFloatState,
    velocityTracker: VelocityTracker,
): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId }

        if (change == null || change.isConsumed) {
            return false
        }

        if (change.changedToUpIgnoreConsumed()) {
            return true
        }

        velocityTracker.addPosition(
            timeMillis = change.uptimeMillis,
            position = change.position,
        )
        offsetX.floatValue += change.positionChange().x
        change.consume()
    }
}

private suspend fun settleSwipe(
    offsetX: MutableFloatState,
    visibilityFraction: Animatable<Float, AnimationVector1D>,
    thresholdPx: Float,
    minFlingDistancePx: Float,
    width: Float,
    velocityX: Float,
    velocityY: Float,
    startToEndAction: ConversationSwipeAction?,
    endToStartAction: ConversationSwipeAction?,
) {
    val direction = resolveSettleDirection(
        offset = offsetX.floatValue,
        thresholdPx = thresholdPx,
        minFlingDistancePx = minFlingDistancePx,
        velocityX = velocityX,
        velocityY = velocityY,
    )

    val action = when (direction) {
        SwipeDirection.StartToEnd -> startToEndAction
        SwipeDirection.EndToStart -> endToStartAction
        SwipeDirection.None -> null
    }

    when {
        action == null -> {
            animateOffset(
                offsetX = offsetX,
                targetValue = 0f,
                initialVelocity = velocityX,
                animationSpec = SwipeSettleSpec,
            )
        }

        action.background.isDismiss -> {
            val target = when (direction) {
                SwipeDirection.StartToEnd -> width
                else -> -width
            }

            animateOffset(
                offsetX = offsetX,
                targetValue = target,
                initialVelocity = velocityX,
                animationSpec = SwipeArchiveSlideSpec,
            )
            visibilityFraction.animateTo(
                targetValue = 0f,
                animationSpec = ItemCollapseSpec,
            )
            action.onTrigger()
        }

        else -> {
            action.onTrigger()
            animateOffset(
                offsetX = offsetX,
                targetValue = 0f,
                initialVelocity = velocityX,
                animationSpec = SwipeSettleSpec,
            )
        }
    }
}

private fun isHorizontalDrag(panOffset: Offset): Boolean {
    return abs(panOffset.x) > abs(panOffset.y) * SWIPE_DIRECTION_BIAS
}

private fun resolveSettleDirection(
    offset: Float,
    thresholdPx: Float,
    minFlingDistancePx: Float,
    velocityX: Float,
    velocityY: Float,
): SwipeDirection {
    val isHorizontalFling = abs(velocityX) >= SWIPE_FLING_VELOCITY_THRESHOLD &&
        abs(velocityX) >= abs(velocityY) * SWIPE_HORIZONTAL_VELOCITY_BIAS
    val isEndToStartFling = isHorizontalFling && offset <= -minFlingDistancePx && velocityX < 0f
    val isStartToEndFling = isHorizontalFling && offset >= minFlingDistancePx && velocityX > 0f

    return when {
        offset <= -thresholdPx || isEndToStartFling -> SwipeDirection.EndToStart
        offset >= thresholdPx || isStartToEndFling -> SwipeDirection.StartToEnd
        else -> SwipeDirection.None
    }
}

private suspend fun animateOffset(
    offsetX: MutableFloatState,
    targetValue: Float,
    initialVelocity: Float = 0f,
    animationSpec: AnimationSpec<Float>,
) {
    Animatable(offsetX.floatValue).animateTo(
        targetValue = targetValue,
        initialVelocity = initialVelocity,
        animationSpec = animationSpec,
    ) {
        offsetX.floatValue = value
    }
}

private fun resolveBackgroundRender(
    offset: Float,
    startToEndAction: ConversationSwipeAction?,
    endToStartAction: ConversationSwipeAction?,
): SwipeBackgroundRender? {
    return when {
        offset > 0f -> startToEndAction?.let {
            SwipeBackgroundRender(
                background = it.background,
                alignment = Alignment.CenterStart,
            )
        }

        offset < 0f -> endToStartAction?.let {
            SwipeBackgroundRender(
                background = it.background,
                alignment = Alignment.CenterEnd,
            )
        }

        else -> null
    }
}

@Composable
private fun ConversationListSwipeBackground(
    render: SwipeBackgroundRender?,
    isUnread: Boolean,
    modifier: Modifier = Modifier,
) {
    if (render == null) {
        Box(modifier = modifier)
        return
    }

    val background = render.background

    val containerColor = when (background) {
        ConversationSwipeBackground.Archive -> MaterialTheme.colorScheme.secondaryContainer
        ConversationSwipeBackground.Unarchive -> MaterialTheme.colorScheme.secondaryContainer
        ConversationSwipeBackground.ToggleRead -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val contentColor = when (background) {
        ConversationSwipeBackground.Archive -> MaterialTheme.colorScheme.onSecondaryContainer
        ConversationSwipeBackground.Unarchive -> MaterialTheme.colorScheme.onSecondaryContainer
        ConversationSwipeBackground.ToggleRead -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Box(
        modifier = modifier
            .clip(SwipeBackgroundShape)
            .background(containerColor)
            .padding(horizontal = SwipeBackgroundHorizontalPadding),
        contentAlignment = render.alignment,
    ) {
        Icon(
            imageVector = swipeBackgroundIcon(background, isUnread),
            contentDescription = swipeBackgroundDescription(background, isUnread),
            tint = contentColor,
        )
    }
}

private fun Modifier.interactionBlocking(isInteractionEnabled: Boolean): Modifier {
    return when {
        isInteractionEnabled -> this
        else -> {
            this
                .consumeAllPointerInput()
                .clearAndSetSemantics {}
        }
    }
}

private fun swipeBackgroundIcon(
    background: ConversationSwipeBackground,
    isUnread: Boolean,
): ImageVector {
    return when (background) {
        ConversationSwipeBackground.Archive -> Icons.Filled.Archive
        ConversationSwipeBackground.Unarchive -> Icons.Filled.Unarchive
        ConversationSwipeBackground.ToggleRead -> when {
            isUnread -> Icons.Filled.MarkChatRead
            else -> Icons.Filled.MarkChatUnread
        }
    }
}

@Composable
private fun swipeBackgroundDescription(
    background: ConversationSwipeBackground,
    isUnread: Boolean,
): String {
    return when (background) {
        ConversationSwipeBackground.Archive -> stringResource(R.string.action_archive)
        ConversationSwipeBackground.Unarchive -> stringResource(R.string.action_unarchive)
        ConversationSwipeBackground.ToggleRead -> when {
            isUnread -> stringResource(R.string.mark_as_read)
            else -> stringResource(R.string.mark_as_unread)
        }
    }
}
