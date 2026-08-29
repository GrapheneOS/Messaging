package com.android.common.test.helpers

import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.BuglePrefsKeys

/**
 * Suppresses the one-off prompts a fresh install shows before the app is usable, so tests land on
 * the screen under test instead of the SMS warning or the self phone number permission dialog.
 */
object FirstRunFlagsHelper {

    private val flagDefaults = mapOf(
        BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED to
            BuglePrefsKeys.SMS_WARNING_ACKNOWLEDGED_DEFAULT,
        BuglePrefsKeys.SELF_PHONE_NUMBER_PERMISSION_REQUESTED to
            BuglePrefsKeys.SELF_PHONE_NUMBER_PERMISSION_REQUESTED_DEFAULT,
    )

    fun suppressFirstRunPrompts(): Map<String, Boolean> {
        val prefs = BuglePrefs.getApplicationPrefs()

        return flagDefaults.mapValues { (key, default) ->
            prefs.getBoolean(key, default).also { prefs.putBoolean(key, true) }
        }
    }

    fun restoreFirstRunPrompts(previousFlags: Map<String, Boolean>) {
        val prefs = BuglePrefs.getApplicationPrefs()

        previousFlags.forEach { (key, value) -> prefs.putBoolean(key, value) }
    }
}
