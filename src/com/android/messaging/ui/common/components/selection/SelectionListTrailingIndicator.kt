package com.android.messaging.ui.common.components.selection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.optionalTestTag

@Composable
internal fun SelectionListTrailingIndicator(
    visible: Boolean,
    testTag: String?,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 200),
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialScale = 0.8f,
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 150),
        ) + scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            targetScale = 0.8f,
        ),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size = 20.dp)
                .optionalTestTag(testTag),
            strokeWidth = 2.dp,
        )
    }
}
