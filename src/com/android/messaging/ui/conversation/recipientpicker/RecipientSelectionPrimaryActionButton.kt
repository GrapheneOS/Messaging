package com.android.messaging.ui.conversation.recipientpicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun RecipientSelectionPrimaryActionButton(
    enabled: Boolean,
    isLoading: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val taggedModifier = when {
        testTag != null -> modifier.testTag(testTag)
        else -> modifier
    }

    Button(
        modifier = taggedModifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(size = 18.dp),
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                recipientSelectionPrimaryActionContentTransform()
            },
            label = "recipientSelectionPrimaryActionButtonContent",
        ) { isButtonLoading ->
            when {
                isButtonLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size = 18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = text)

                        Spacer(modifier = Modifier.size(size = 8.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private fun recipientSelectionPrimaryActionContentTransform(): ContentTransform {
    return (
        fadeIn(
            animationSpec = tween(durationMillis = 200),
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialScale = 0.9f,
        )
        ).togetherWith(
        fadeOut(
            animationSpec = tween(durationMillis = 150),
        ) + scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            targetScale = 0.9f,
        ),
    )
}
