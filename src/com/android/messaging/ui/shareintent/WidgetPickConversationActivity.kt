package com.android.messaging.ui.shareintent

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.shareintent.screen.ShareIntentScreen
import com.android.messaging.ui.shareintent.screen.WidgetPickEffectHandler
import com.android.messaging.widget.WidgetConversationPrefs
import com.android.messaging.widget.WidgetConversationProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetPickConversationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = resolveAppWidgetId()
        if (!isValidConfigureRequest()) {
            finish()
            return
        }

        enableEdgeToEdge()

        val effectHandler = WidgetPickEffectHandler(
            onConversationPicked = ::bindConversationToWidget,
        )

        setContent {
            AppTheme {
                ShareIntentScreen(
                    allowMultiSelect = false,
                    isInitialDraftLoading = false,
                    initialDraft = null,
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
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

    private fun bindConversationToWidget(conversationId: String) {
        WidgetConversationPrefs.saveConversationIdPref(appWidgetId, conversationId)
        WidgetConversationProvider.rebuildWidget(this, appWidgetId)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
