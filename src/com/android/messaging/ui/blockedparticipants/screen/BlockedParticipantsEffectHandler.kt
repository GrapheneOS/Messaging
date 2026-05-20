package com.android.messaging.ui.blockedparticipants.screen

import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect as Effect

internal interface BlockedParticipantsEffectHandler {
    fun handle(effect: Effect)
}

internal class BlockedParticipantsEffectHandlerImpl : BlockedParticipantsEffectHandler {

    override fun handle(effect: Effect) {
    }
}
