package com.android.messaging.ui.conversationpicker.host.forward

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationpicker.ConversationPickerEffectHandler
import com.android.messaging.ui.conversationpicker.ConversationPickerScreen
import com.android.messaging.ui.conversationpicker.ConversationPickerScreenModel
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.core.CollectEvents

@Composable
internal fun ForwardMessageScreen(
    screenModel: ForwardMessageViewModel,
    pickerScreenModel: ConversationPickerScreenModel,
    effectHandler: ConversationPickerEffectHandler,
    onOpenConversation: (ConversationId) -> Unit,
    onClose: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    CollectEvents(events = screenModel.navigationEvents) { event ->
        when (event) {
            is ForwardMessageNavEvent.OpenConversation -> onOpenConversation(event.conversationId)
            is ForwardMessageNavEvent.Close -> onClose()
        }
    }

    ConversationPickerScreen(
        screenModel = pickerScreenModel,
        isInitialDraftLoading = uiState.isLoading,
        initialDraft = uiState.draft,
        effectHandler = effectHandler,
        onNavigateBack = onNavigateBack,
        allowMultiSelect = true,
        labels = ConversationPickerLabels.Forward,
        modifier = modifier,
    )
}
