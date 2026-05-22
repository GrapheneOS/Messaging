package com.android.messaging.ui.blockedparticipants.screen

import android.app.Activity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect as Effect
import com.android.messaging.util.UiUtils

internal interface BlockedParticipantsEffectHandler {
    fun handle(effect: Effect)
}

internal class BlockedParticipantsEffectHandlerImpl(
    private val activity: Activity,
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
        }
    }
}
