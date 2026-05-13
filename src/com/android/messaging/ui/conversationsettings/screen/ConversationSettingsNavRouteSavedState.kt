package com.android.messaging.ui.conversationsettings.screen

import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavRoute
import kotlinx.parcelize.Parcelize

internal sealed interface ConversationSettingsNavRouteSavedState : Parcelable {

    @Parcelize
    data object Conversation : ConversationSettingsNavRouteSavedState

    @Parcelize
    data class ParticipantInfo(
        val conversationId: String,
    ) : ConversationSettingsNavRouteSavedState

    companion object {
        val Saver: Saver<ConversationSettingsNavRoute, ConversationSettingsNavRouteSavedState> =
            Saver(
                save = { route -> route.toSavedState() },
                restore = { savedState -> savedState.toRoute() },
            )
    }
}

private fun ConversationSettingsNavRoute.toSavedState(): ConversationSettingsNavRouteSavedState {
    return when (this) {
        ConversationSettingsNavRoute.Conversation -> {
            ConversationSettingsNavRouteSavedState.Conversation
        }

        is ConversationSettingsNavRoute.ParticipantInfo -> {
            ConversationSettingsNavRouteSavedState.ParticipantInfo(
                conversationId = conversationId,
            )
        }
    }
}

private fun ConversationSettingsNavRouteSavedState.toRoute(): ConversationSettingsNavRoute {
    return when (this) {
        ConversationSettingsNavRouteSavedState.Conversation -> {
            ConversationSettingsNavRoute.Conversation
        }

        is ConversationSettingsNavRouteSavedState.ParticipantInfo -> {
            ConversationSettingsNavRoute.ParticipantInfo(
                conversationId = conversationId,
            )
        }
    }
}
