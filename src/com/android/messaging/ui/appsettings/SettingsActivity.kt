package com.android.messaging.ui.appsettings

import android.content.Intent
import android.os.Bundle
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.MainActivity
import com.android.messaging.ui.appsettings.navigation.UI_INTENT_EXTRA_GOTO_SETTINGS
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BugleComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(UI_INTENT_EXTRA_GOTO_SETTINGS, true)
            },
        )

        finish()
    }
}
