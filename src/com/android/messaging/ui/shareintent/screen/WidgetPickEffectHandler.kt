package com.android.messaging.ui.shareintent.screen

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.Intent
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect
import com.android.messaging.widget.WidgetConversationPrefs
import com.android.messaging.widget.WidgetConversationProvider

internal class WidgetPickEffectHandler(
    private val activity: Activity,
    private val appWidgetId: Int,
) : ShareIntentEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenConversation -> {
                bindConversationToWidget(effect.conversationId)
            }

            is Effect.SendToSelected -> Unit
        }
    }

    private fun bindConversationToWidget(conversationId: String) {
        WidgetConversationPrefs.saveConversationIdPref(appWidgetId, conversationId)
        WidgetConversationProvider.rebuildWidget(activity, appWidgetId)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        activity.setResult(RESULT_OK, resultValue)
        activity.finish()
    }
}
