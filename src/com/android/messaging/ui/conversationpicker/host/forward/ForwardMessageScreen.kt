package com.android.messaging.ui.conversationpicker.host.forward

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationpicker.ConversationPickerScreen
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.core.CollectEvents
import com.android.messaging.util.UiUtils

@Composable
internal fun ForwardMessageScreen(
    onOpenConversation: (ConversationId) -> Unit,
    onClose: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ForwardMessageViewModel = hiltViewModel<ForwardMessageViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    val effectHandler = rememberForwardMessageEffectHandler(
        onTargetSelected = screenModel::onTargetSelected,
        onSendToSelected = { targets, draft ->
            screenModel.onSendToTargets(
                targets = targets,
                draft = draft,
            ) {
                UiUtils.showToastAtBottom(R.string.send_message_failure)
            }
        },
    )

    CollectEvents(events = screenModel.navigationEvents) { event ->
        when (event) {
            is ForwardMessageNavEvent.OpenConversation -> onOpenConversation(event.conversationId)
            is ForwardMessageNavEvent.Close -> onClose()
        }
    }

    ConversationPickerScreen(
        isInitialDraftLoading = uiState.isLoading,
        initialDraft = uiState.draft,
        effectHandler = effectHandler,
        onNavigateBack = onNavigateBack,
        allowMultiSelect = true,
        labels = ConversationPickerLabels.Forward,
        modifier = modifier,
    )
}
