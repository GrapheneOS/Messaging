package com.android.messaging.ui.blockedparticipants.screen

import android.app.Activity
import android.graphics.Point
import android.net.Uri
import android.view.View
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect as Effect
import com.android.messaging.util.ContactUtil
import com.android.messaging.util.UiUtils

internal interface BlockedParticipantsEffectHandler {
    fun handle(effect: Effect)
}

internal class BlockedParticipantsEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
) : BlockedParticipantsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.ShowMessage -> {
                UiUtils.showToastAtBottom(effect.messageResId)
            }

            is Effect.OpenParticipantChat -> {
                UIIntents.get().launchConversationActivity(
                    activity,
                    effect.conversationId,
                    null,
                )
                activity.finish()
            }

            is Effect.PlacePhoneCall -> {
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
                    effect.contactLookupKey,
                    effect.avatarUri?.let(Uri::parse),
                    effect.normalizedDestination,
                )
            }
        }
    }
}
