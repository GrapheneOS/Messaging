package com.android.messaging.ui.conversationpicker.host.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.conversationpicker.ConversationPickerScreen
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetPickConversationActivity : BugleComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        setResult(RESULT_CANCELED)

        appWidgetId = resolveAppWidgetId()
        if (!isValidConfigureRequest()) {
            finish()
            return
        }

        enableEdgeToEdge()

        val effectHandler = WidgetPickEffectHandler(
            activity = this,
            appWidgetId = appWidgetId,
        )

        setContent {
            AppTheme {
                ConversationPickerScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                    allowMultiSelect = false,
                    labels = ConversationPickerLabels.Widget,
                    isInitialDraftLoading = false,
                    initialDraft = null,
                )
            }
        }
    }

    private fun resolveAppWidgetId(): Int {
        return intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun isValidConfigureRequest(): Boolean {
        return appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
            intent.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
    }
}
