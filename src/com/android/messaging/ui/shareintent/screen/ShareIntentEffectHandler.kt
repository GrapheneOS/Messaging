package com.android.messaging.ui.shareintent.screen

import android.app.Activity
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect

internal interface ShareIntentEffectHandler {
    fun handle(effect: Effect)
}

internal class ShareIntentEffectHandlerImpl(
    private val activity: Activity,
) : ShareIntentEffectHandler {

    override fun handle(effect: Effect) {
    }
}
