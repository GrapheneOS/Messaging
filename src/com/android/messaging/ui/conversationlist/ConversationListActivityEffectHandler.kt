package com.android.messaging.ui.conversationlist

import android.app.Activity
import android.graphics.Point
import android.view.View
import androidx.core.net.toUri
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationlist.redesign.ConversationListEffectHandler
import com.android.messaging.ui.conversationlist.redesign.model.ConversationListEffect as Effect
import com.android.messaging.util.ContactUtil
import com.android.messaging.util.DebugUtils

internal class ConversationListActivityEffectHandler(
    private val activity: Activity,
    private val hostView: View,
) : ConversationListEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            Effect.StartChat -> {
                UIIntents.get().launchCreateNewConversationActivity(
                    activity,
                    null,
                )
            }

            is Effect.OpenConversation -> {
                UIIntents.get().launchConversationActivity(
                    activity,
                    effect.conversationId,
                    null,
                )
            }

            is Effect.OpenConversationSettings -> {
                UIIntents.get().launchPeopleAndOptionsActivity(
                    activity,
                    effect.conversationId,
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

            is Effect.PlaceCall -> {
                UIIntents.get().launchPhoneCallActivity(
                    activity,
                    effect.destination,
                    Point(0, 0),
                )
            }

            is Effect.ShowOrAddContact -> {
                ContactUtil.showOrAddContact(
                    hostView,
                    effect.contactId,
                    effect.lookupKey,
                    effect.avatarUri?.toUri(),
                    effect.destination,
                )
            }

            else -> Unit
        }
    }
}
