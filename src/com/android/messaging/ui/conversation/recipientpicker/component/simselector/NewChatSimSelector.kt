package com.android.messaging.ui.conversation.recipientpicker.component.simselector

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.preview.previewSimSelectorUiState
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.subscription.component.SimSelectorRow
import com.android.messaging.ui.subscription.mapper.rememberSimSelectorUiState

@Composable
internal fun NewChatSimSelectorRow(
    uiState: ConversationSimSelectorUiState,
    onSimSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val simSelectorUiState = rememberSimSelectorUiState(
        subscriptions = uiState.subscriptions,
        selectedSelfParticipantId = uiState.selectedSubscription?.selfParticipantId,
    )

    val selectedLabel = simSelectorUiState.selectedOption?.label.orEmpty()

    SimSelectorRow(
        modifier = modifier,
        uiState = simSelectorUiState,
        prefixText = stringResource(id = R.string.new_chat_sim_selector_prefix),
        chipContentDescription = stringResource(
            id = R.string.new_chat_sim_selector_chip_content_description,
            selectedLabel,
        ),
        selectedContentDescription = stringResource(id = R.string.sim_selector_item_selected),
        onSimSelected = onSimSelected,
    )
}

@PreviewLightDark
@Composable
private fun NewChatSimSelectorRowPreview() {
    MessagingPreviewColumn {
        NewChatSimSelectorRow(
            uiState = previewSimSelectorUiState(),
            onSimSelected = { _ -> },
        )
    }
}
