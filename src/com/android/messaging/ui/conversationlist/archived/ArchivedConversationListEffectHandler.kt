package com.android.messaging.ui.conversationlist.archived

import android.app.Activity
import android.graphics.Point
import android.view.View
import androidx.core.net.toUri
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListEffect as Effect
import com.android.messaging.util.ContactUtil
import com.android.messaging.util.DebugUtils

internal interface ArchivedConversationListEffectHandler {
    fun handle(effect: Effect)
}

internal class ArchivedConversationListEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
) : ArchivedConversationListEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
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

            Effect.OpenDebugOptions -> {
                DebugUtils.showDebugOptions(activity)
            }

            is Effect.ConversationsUnarchived -> Unit
        }
    }
}
