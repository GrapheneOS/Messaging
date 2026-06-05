package com.android.messaging.widget

import com.android.messaging.Factory
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.BuglePrefs

object WidgetConversationPrefs {

    @JvmStatic
    fun saveConversationIdPref(appWidgetId: Int, conversationId: String) {
        widgetPrefs()?.putString(key(appWidgetId), conversationId)
    }

    @JvmStatic
    fun getConversationIdPref(appWidgetId: Int): String? {
        return widgetPrefs()?.getString(key(appWidgetId), null)
    }

    @JvmStatic
    fun deleteConversationIdPref(appWidgetId: Int) {
        widgetPrefs()?.remove(key(appWidgetId))
    }

    private fun widgetPrefs(): BuglePrefs? {
        return Factory.get().widgetPrefs
    }

    private fun key(appWidgetId: Int): String {
        return UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID + appWidgetId
    }
}
