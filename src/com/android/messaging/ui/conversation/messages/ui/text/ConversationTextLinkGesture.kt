package com.android.messaging.ui.conversation.messages.ui.text

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect

private const val TOUCH_POINT_RADIUS = 0.005f

internal class ConversationTextLinkGesture {
    var textLayoutResult: TextLayoutResult? = null
    var coordinates: LayoutCoordinates? = null

    var blocksClick: Boolean = false
        private set

    fun update(blocked: Boolean) {
        blocksClick = blocked
    }
}

internal fun Modifier.selectConversationTextLinkOnLongPress(
    gesture: ConversationTextLinkGesture,
    state: ConversationTextSelectionState,
    hapticFeedback: HapticFeedback,
): Modifier {
    return pointerInput(gesture, state, hapticFeedback) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val isLinkPress = !down.isConsumed && awaitLinkPress(down = down)
            val layout = gesture.textLayoutResult
            val link = layout?.takeIf { isLinkPress }?.pressedLink(
                position = down.position,
                state = state,
                minimumTouchTargetSize = viewConfiguration.minimumTouchTargetSize.toSize(),
            )
            val canSelect = state.enabled && down.type != PointerType.Mouse
            if (canSelect && link != null && awaitConversationLongPress(down = down)) {
                gesture.update(blocked = true)
                try {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.selection.select(range = link)
                    dragLinkSelection(
                        down = down,
                        layout = layout,
                        link = link,
                        state = state,
                        gesture = gesture,
                    )
                } finally {
                    gesture.update(blocked = false)
                }
            }
        }
    }
}

// Links are children of the text, so by the Main pass a link has consumed the down it handles.
private suspend fun AwaitPointerEventScope.awaitLinkPress(down: PointerInputChange): Boolean {
    val event = awaitPointerEvent(pass = PointerEventPass.Main)
    return event.changes.any { it.id == down.id && it.isConsumed }
}

// Matches Compose's hit test: the topmost link whose path contains the touch takes it, otherwise
// the nearest link whose minimum touch target does. Later links are drawn on top.
private fun TextLayoutResult.pressedLink(
    position: Offset,
    state: ConversationTextSelectionState,
    minimumTouchTargetSize: Size,
): TextRange? {
    val linkPaths = state.links
        .filter { link -> link.end <= layoutInput.text.length }
        .map { link -> link to getPathForRange(start = link.start, end = link.end) }
        .asReversed()
    val directHit = linkPaths.firstOrNull { (_, path) -> path.contains(position = position) }?.first
    val pressed = directHit ?: linkPaths
        .map { (link, path) ->
            link to path.getBounds().roundToIntRect().touchTargetDistanceSquared(
                point = position,
                minimumTouchTargetSize = minimumTouchTargetSize,
            )
        }
        .filter { (_, distance) -> distance.isFinite() }
        .minByOrNull { (_, distance) -> distance }
        ?.first
    return pressed?.let { link ->
        TextRange(start = link.start, end = link.end).offsetBy(offset = state.bodyTextOffset)
    }
}

// Compose's clip test intersects the path with a tiny square, so a line's bottom edge counts.
private fun Path.contains(position: Offset): Boolean {
    val touchPoint = Path().apply {
        addRect(rect = Rect(center = position, radius = TOUCH_POINT_RADIUS))
    }
    return !Path.combine(operation = PathOperation.Intersect, path1 = this, path2 = touchPoint)
        .isEmpty
}

// A link smaller than the minimum touch target only takes touch within that target, and only along
// the dimensions it falls short in.
private fun IntRect.touchTargetDistanceSquared(point: Offset, minimumTouchTargetSize: Size): Float {
    val horizontalPadding = maxOf(0f, (minimumTouchTargetSize.width - width) / 2)
    val verticalPadding = maxOf(0f, (minimumTouchTargetSize.height - height) / 2)
    val horizontal = maxOf(left - point.x, 0f, point.x - right)
    val vertical = maxOf(top - point.y, 0f, point.y - bottom)
    return when {
        horizontalPadding == 0f && verticalPadding == 0f -> Float.POSITIVE_INFINITY
        horizontal > horizontalPadding || vertical > verticalPadding -> Float.POSITIVE_INFINITY
        else -> horizontal * horizontal + vertical * vertical
    }
}

internal suspend fun AwaitPointerEventScope.awaitConversationLongPress(
    down: PointerInputChange,
): Boolean {
    val cancelled = withTimeoutOrNull(timeMillis = viewConfiguration.longPressTimeoutMillis) {
        var waiting = true
        while (waiting) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val change = event.changes.singleOrNull { it.id == down.id }
            waiting = event.changes.size == 1 && change != null && change.pressed &&
                !change.isConsumed &&
                (change.position - down.position).getDistance() <= viewConfiguration.touchSlop
        }

        true
    }

    return cancelled == null
}

private suspend fun AwaitPointerEventScope.dragLinkSelection(
    down: PointerInputChange,
    layout: TextLayoutResult,
    link: TextRange,
    state: ConversationTextSelectionState,
    gesture: ConversationTextLinkGesture,
) {
    var pressed = true
    var previousRange = link
    while (pressed) {
        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change != null) {
            val positionInRoot = gesture.coordinates?.takeIf { it.isAttached }
                ?.localToRoot(relativeToLocal = change.position)

            val subjectWord = positionInRoot?.let { state.subjectWordAt(positionInRoot = it) }

            val currentWord = subjectWord ?: layout
                .getWordBoundary(
                    offset = layout.getOffsetForPosition(position = change.position),
                ).offsetBy(offset = state.bodyTextOffset)

            // A press beside a link goes to its touch target, so the word there isn't the link's.
            val hasDragged =
                (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            val range = when {
                !hasDragged -> link
                currentWord.start < link.start -> TextRange(
                    start = link.end,
                    end = currentWord.start,
                )
                else -> TextRange(start = link.start, end = maxOf(link.end, currentWord.end))
            }

            // SelectionContainer clears a stationary pointer's selection on up, before its
            // children receive Initial. Reapply our range as part of this same gesture.
            if (range != previousRange || !state.hasSelection) {
                state.selection.select(range = range)
                previousRange = range
            }
            change.consume()
        }
        pressed = change?.pressed == true && event.changes.size == 1
        // Keep suppression through Main, where LinkAnnotation dispatches its click.
        awaitPointerEvent(pass = PointerEventPass.Final)
    }
}

private fun TextRange.offsetBy(offset: Int): TextRange {
    return TextRange(start = start + offset, end = end + offset)
}
