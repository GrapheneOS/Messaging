package com.android.messaging.ui.conversation.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.messaging.ui.conversation.entry.model.NewChatEffect as Effect
import com.android.messaging.util.UiUtils

@Composable
internal fun rememberNewChatEffectHandler(): NewChatEffectHandler {
    return remember { NewChatEffectHandlerImpl() }
}

internal interface NewChatEffectHandler {
    fun handle(effect: Effect)
}

internal class NewChatEffectHandlerImpl : NewChatEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.ShowMessage -> {
                UiUtils.showToastAtBottom(effect.messageResId)
            }
        }
    }
}
