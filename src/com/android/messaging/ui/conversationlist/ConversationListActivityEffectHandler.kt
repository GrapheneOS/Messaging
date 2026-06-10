package com.android.messaging.ui.conversationlist

import androidx.activity.ComponentActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationlist.redesign.ConversationListEffectHandler
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListEffect as Effect
import com.android.messaging.util.DebugUtils

internal class ConversationListActivityEffectHandler(
    private val activity: ComponentActivity,
) : ConversationListEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            Effect.StartChat -> {
                UIIntents.get().launchCreateNewConversationActivity(
                    activity,
                    null
                )
            }

            is Effect.OpenConversation -> {
                UIIntents.get().launchConversationActivity(
                    activity,
                    effect.conversationId,
                    null,
                )
            }

            Effect.OpenArchivedConversations -> {
                UIIntents.get().launchArchivedConversationsActivity(activity)
            }

            Effect.OpenBlockedParticipants -> {
                UIIntents.get().launchBlockedParticipantsActivity(activity)
            }

            Effect.OpenSettings -> {
                UIIntents.get().launchSettingsActivity(activity)
            }

            Effect.OpenDebugOptions -> {
                DebugUtils.showDebugOptions(activity)
            }

            is Effect.OpenAddContact -> {
                UIIntents.get().launchAddContactActivity(
                    activity,
                    effect.destination,
                )
            }

            is Effect.ConversationBlocked,
            is Effect.ConfirmAddContact,
            is Effect.ConfirmBlock,
            is Effect.ConversationsArchived,
            Effect.ScrollToTop,
            -> Unit
        }
    }
}
