package com.android.messaging.ui.blockedparticipants.screen

import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect as Effect
import com.android.messaging.util.UiUtils

internal interface BlockedParticipantsEffectHandler {
    fun handle(effect: Effect)
}

internal class BlockedParticipantsEffectHandlerImpl : BlockedParticipantsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.ShowMessage -> {
                UiUtils.showToastAtBottom(effect.messageResId)
            }
        }
    }
}
