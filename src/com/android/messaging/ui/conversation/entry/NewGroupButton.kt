package com.android.messaging.ui.conversation.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.conversation.NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG
import com.android.messaging.ui.core.MessagingPreviewColumn

@Composable
internal fun NewChatRecipientSelectionTopListContent(
    isCreatingGroup: Boolean,
    onCreateGroupClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = !isCreatingGroup,
        enter = newGroupButtonEnterTransition(),
        exit = newGroupButtonExitTransition(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            NewGroupButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreateGroupClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NewGroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    FilledTonalButton(
        modifier = modifier
            .testTag(tag = NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG),
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(
                alpha = 0.5f,
            ),
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                alpha = 0.5f,
            ),
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Group,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.size(size = 8.dp))
        Text(text = stringResource(id = R.string.conversation_new_group))
    }
}

private fun newGroupButtonEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = newChatDefaultEffectsAnimationSpec(),
    ) + slideInVertically(
        animationSpec = newChatSpatialAnimationSpec(),
        initialOffsetY = { fullHeight ->
            -fullHeight / 4
        },
    )
}

private fun newGroupButtonExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = newChatFastEffectsAnimationSpec(),
    ) + shrinkVertically(
        animationSpec = newChatSpatialAnimationSpec(),
        shrinkTowards = Alignment.Top,
    )
}

private fun <T> newChatDefaultEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing,
    )
}

private fun <T> newChatFastEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )
}

private fun <T> newChatSpatialAnimationSpec(): FiniteAnimationSpec<T> {
    return spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

@PreviewLightDark
@Composable
private fun NewChatRecipientSelectionTopListContentPreview() {
    MessagingPreviewColumn {
        NewChatRecipientSelectionTopListContent(
            isCreatingGroup = false,
            onCreateGroupClick = {},
        )
    }
}
