package com.android.messaging.ui.appsettings.navigation

import android.content.Intent

internal const val UI_INTENT_EXTRA_GOTO_SETTINGS = "goto_settings"

internal fun Intent.goToSettings(): Boolean {
    return getBooleanExtra(UI_INTENT_EXTRA_GOTO_SETTINGS, false)
}
