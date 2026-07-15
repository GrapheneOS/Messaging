package com.android.messaging.ui.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val ItemHorizontalPadding = 8.dp
private val ItemVerticalPadding = 8.dp
private val ListRowShape = RoundedCornerShape(percent = 50)

@Composable
internal fun TwoLineListItem(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = ListRowShape,
    color: Color = MaterialTheme.colorScheme.background,
    contentDescription: String? = null,
    keepLeadingContentAccessible: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    TwoLineListItem(
        onClick = onClick,
        leadingContent = leadingContent,
        titleContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        onLongClick = onLongClick,
        shape = shape,
        color = color,
        contentDescription = contentDescription,
        keepLeadingContentAccessible = keepLeadingContentAccessible,
        subtitleContent = when {
            subtitle.isNullOrBlank() -> null

            else -> {
                {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        trailingContent = trailingContent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TwoLineListItem(
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
    titleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = ListRowShape,
    color: Color = MaterialTheme.colorScheme.background,
    contentDescription: String? = null,
    keepLeadingContentAccessible: Boolean = false,
    subtitleContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = color,
    ) {
        val clickModifier = when (onLongClick) {
            null -> Modifier.clickable(onClick = onClick)
            else -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        }
        val semanticsModifier = when (contentDescription) {
            null -> Modifier
            else -> Modifier.semantics {
                this.contentDescription = contentDescription
            }
        }
        val descendantSemanticsModifier = when (contentDescription) {
            null -> Modifier
            else -> Modifier.clearAndSetSemantics {}
        }
        val leadingSemanticsModifier = when {
            contentDescription == null -> Modifier
            keepLeadingContentAccessible -> Modifier
            else -> descendantSemanticsModifier
        }

        Row(
            modifier = clickModifier
                .then(semanticsModifier)
                .padding(
                    horizontal = ItemHorizontalPadding,
                    vertical = ItemVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = leadingSemanticsModifier) {
                leadingContent()
            }

            Spacer(modifier = Modifier.width(ItemHorizontalPadding))

            TwoLineListItemContent(
                titleContent = titleContent,
                subtitleContent = subtitleContent,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ItemHorizontalPadding)
                    .then(descendantSemanticsModifier),
            )

            trailingContent?.let { content ->
                Box(modifier = descendantSemanticsModifier) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun TwoLineListItemContent(
    titleContent: @Composable () -> Unit,
    subtitleContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val twoLineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp() +
            MaterialTheme.typography.bodySmall.lineHeight.toDp()
    }

    Column(
        modifier = modifier.heightIn(min = twoLineHeight),
        verticalArrangement = Arrangement.Center,
    ) {
        titleContent()
        subtitleContent?.invoke()
    }
}
