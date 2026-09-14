package com.android.messaging.ui.conversation.addparticipants

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.messaging.ui.conversation.addparticipants.model.AddParticipantsEffect as Effect
import com.android.messaging.util.UiUtils

@Composable
internal fun rememberAddParticipantsEffectHandler(): AddParticipantsEffectHandler {
    return remember { AddParticipantsEffectHandlerImpl() }
}

internal interface AddParticipantsEffectHandler {
    fun handle(effect: Effect)
}

internal class AddParticipantsEffectHandlerImpl : AddParticipantsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.ShowMessage -> {
                UiUtils.showToastAtBottom(effect.messageResId)
            }
        }
    }
}
