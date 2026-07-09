package com.android.messaging.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp


@Composable
internal fun PagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) {
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(times = pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val dotColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                else -> {
                    MaterialTheme.colorScheme.onSurfaceVariant
                        .copy(alpha = 0.4f)
                }
            }

            Box(
                modifier = Modifier
                    .size(size = 8.dp)
                    .clip(shape = CircleShape)
                    .background(color = dotColor),
            )
        }
    }
}
