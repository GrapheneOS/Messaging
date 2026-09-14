package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.runtime.Composable
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction as Action
import com.android.messaging.ui.conversationlist.common.dialog.ConversationListDeleteDialog

@Composable
internal fun BlockedParticipantsDialogs(
    selectedCount: Int,
    onAction: (Action) -> Unit,
    showDeleteConfirmation: Boolean,
    onDismissDeleteConfirmation: () -> Unit,
) {
    if (showDeleteConfirmation) {
        ConversationListDeleteDialog(
            selectedCount = selectedCount,
            onConfirm = {
                onAction(Action.DeleteSelectedConfirmed)
                onDismissDeleteConfirmation()
            },
            onDismiss = onDismissDeleteConfirmation,
        )
    }
}
