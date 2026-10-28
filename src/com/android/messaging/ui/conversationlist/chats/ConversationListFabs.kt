package com.android.messaging.ui.conversationlist.chats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.ui.common.components.ComposeBarControlHeight
import com.android.messaging.ui.common.components.ComposeBarVerticalPadding
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.common.components.PrimaryActionButton
import com.android.messaging.ui.common.components.horizontalSafeDrawingInsets
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.ConversationListUiState as State
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState

private val FabSpacing = 16.dp

@Composable
internal fun BoxScope.ConversationListFabs(
    uiState: State,
    isSelectionMode: Boolean,
    onAction: (Action) -> Unit,
    onScrollToTop: () -> Unit,
) {
    val hasItems = uiState.content is ConversationListContentUiState.Items
    val fabWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
    val horizontalInsets = horizontalSafeDrawingInsets()
    val bottomSpacing = when {
        LocalIsListDetailPane.current -> ComposeBarVerticalPadding
        else -> FabSpacing
    }

    ScrollToTopFab(
        visible = uiState.isScrollToTopVisible,
        onClick = onScrollToTop,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(fabWindowInsets)
            .padding(bottom = bottomSpacing),
    )

    StartChatFab(
        visible = hasItems && !isSelectionMode,
        expanded = !uiState.isScrollToTopVisible,
        onClick = { onAction(Action.StartChatClicked) },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(fabWindowInsets)
            .padding(horizontalInsets)
            .padding(horizontal = FabSpacing)
            .padding(bottom = bottomSpacing),
    )
}

@Composable
private fun ScrollToTopFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = stringResource(R.string.conversation_list_scroll_to_top),
            )
        }
    }
}

@Composable
private fun StartChatFab(
    visible: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        PrimaryActionButton(
            text = stringResource(R.string.conversation_list_start_chat),
            onClick = onClick,
            modifier = Modifier.height(height = ComposeBarControlHeight),
            expanded = expanded,
            leadingIcon = Icons.AutoMirrored.Rounded.Chat,
        )
    }
}
