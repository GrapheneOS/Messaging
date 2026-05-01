package com.android.messaging.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel

@Composable
internal fun ConversationSubscriptionLabel.resolveDisplayName(): String {
    return when (this) {
        is ConversationSubscriptionLabel.Named -> name

        is ConversationSubscriptionLabel.Slot -> {
            stringResource(
                id = R.string.sim_slot_identifier,
                slotId.toString(),
            )
        }

        is ConversationSubscriptionLabel.DebugFake -> {
            stringResource(
                id = R.string.debug_emulated_sim_display_name,
                slotId.toString(),
            )
        }
    }
}
