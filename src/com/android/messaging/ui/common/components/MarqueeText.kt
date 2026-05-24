package com.android.messaging.ui.common.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val VerticalPadding = 1.dp
private const val MAX_FADE_FRACTION = 0.5f

@Composable
internal fun MarqueeText(
    text: String,
    style: TextStyle,
    color: Color,
    fadeEdgeWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VerticalPadding)
            .horizontalFadingEdges(fadeEdgeWidth),
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            modifier = Modifier
                .basicMarquee()
                .padding(horizontal = fadeEdgeWidth),
        )
    }
}

internal fun Modifier.horizontalFadingEdges(fadeWidth: Dp): Modifier {
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fadePx = fadeWidth.toPx()
            if (fadePx <= 0f || size.width <= 0f) return@drawWithContent
            val fadeFraction = (fadePx / size.width).coerceIn(0f, MAX_FADE_FRACTION)

            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    fadeFraction to Color.Black,
                    1f - fadeFraction to Color.Black,
                    1f to Color.Transparent,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
}
