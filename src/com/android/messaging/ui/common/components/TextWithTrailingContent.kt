package com.android.messaging.ui.common.components

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
internal fun TextWithTrailingContent(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    trailingSpacing: Dp = 0.dp,
    trailingContent: (@Composable () -> Unit)? = null,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    var textLineRightPx by remember(text, inlineContent) {
        mutableIntStateOf(value = 0)
    }
    val measurePolicy = textWithTrailingContentMeasurePolicy(
        textLineRightPx = textLineRightPx,
        trailingSpacing = trailingSpacing,
    )

    Layout(
        modifier = modifier,
        content = {
            Text(
                text = text,
                inlineContent = inlineContent,
                color = color,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                style = style,
                maxLines = maxLines,
                overflow = overflow,
                onTextLayout = { layoutResult ->
                    textLineRightPx = when {
                        layoutResult.lineCount > 0 -> {
                            ceil(layoutResult.getLineRight(lineIndex = 0)).toInt()
                        }

                        else -> 0
                    }
                },
            )

            trailingContent?.invoke()
        },
        measurePolicy = measurePolicy,
    )
}

private fun textWithTrailingContentMeasurePolicy(
    textLineRightPx: Int,
    trailingSpacing: Dp,
): MeasurePolicy {
    return MeasurePolicy { measurables, constraints ->
        val textMeasurable = measurables.firstOrNull() ?: return@MeasurePolicy layout(
            width = constraints.minWidth,
            height = constraints.minHeight,
            placementBlock = {},
        )

        val spacingPx = trailingSpacing.roundToPx()
        val trailingPlaceable = measureTrailingPlaceable(
            measurables = measurables,
            constraints = constraints,
        )
        val reservedTrailingWidth = reservedTrailingWidth(
            trailingPlaceable = trailingPlaceable,
            spacingPx = spacingPx,
        )
        val textPlaceable = measureTextPlaceable(
            textMeasurable = textMeasurable,
            constraints = constraints,
            reservedTrailingWidth = reservedTrailingWidth,
        )
        val visualTextWidth = visualTextWidth(
            textLineRightPx = textLineRightPx,
            textPlaceable = textPlaceable,
        )
        val width = layoutWidth(
            visualTextWidth = visualTextWidth,
            reservedTrailingWidth = reservedTrailingWidth,
            constraints = constraints,
        )
        val height = layoutHeight(
            textPlaceable = textPlaceable,
            trailingPlaceable = trailingPlaceable,
            constraints = constraints,
        )

        layout(
            width = width,
            height = height,
        ) {
            val textY = (height - textPlaceable.height) / 2

            textPlaceable.placeRelative(
                x = 0,
                y = textY,
            )

            trailingPlaceable?.let { placeable ->
                val trailingX = visualTextWidth + spacingPx
                val trailingY = (height - placeable.height) / 2

                placeable.placeRelative(
                    x = trailingX,
                    y = trailingY,
                )
            }
        }
    }
}

private fun measureTrailingPlaceable(
    measurables: List<Measurable>,
    constraints: Constraints,
): Placeable? {
    return measurables
        .getOrNull(index = 1)
        ?.measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
            ),
        )
}

private fun reservedTrailingWidth(
    trailingPlaceable: Placeable?,
    spacingPx: Int,
): Int {
    return when (trailingPlaceable) {
        null -> 0
        else -> trailingPlaceable.width + spacingPx
    }
}

private fun measureTextPlaceable(
    textMeasurable: Measurable,
    constraints: Constraints,
    reservedTrailingWidth: Int,
): Placeable {
    val textMaxWidth = (constraints.maxWidth - reservedTrailingWidth)
        .coerceAtLeast(minimumValue = 0)

    return textMeasurable.measure(
        constraints.copy(
            minWidth = 0,
            maxWidth = textMaxWidth,
            minHeight = 0,
        ),
    )
}

private fun visualTextWidth(
    textLineRightPx: Int,
    textPlaceable: Placeable,
): Int {
    return when {
        textLineRightPx > 0 -> textLineRightPx.coerceIn(
            minimumValue = 0,
            maximumValue = textPlaceable.width,
        )

        else -> textPlaceable.width
    }
}

private fun layoutWidth(
    visualTextWidth: Int,
    reservedTrailingWidth: Int,
    constraints: Constraints,
): Int {
    return (visualTextWidth + reservedTrailingWidth).coerceIn(
        minimumValue = constraints.minWidth,
        maximumValue = constraints.maxWidth,
    )
}

private fun layoutHeight(
    textPlaceable: Placeable,
    trailingPlaceable: Placeable?,
    constraints: Constraints,
): Int {
    return maxOf(
        textPlaceable.height,
        trailingPlaceable?.height ?: 0,
        constraints.minHeight,
    )
}
