package com.android.messaging.debug

import android.app.Activity
import android.os.Bundle
import com.android.messaging.ui.UIIntents

/**
 * Usage:
 *
 * adb shell 'am start -n com.android.messaging.debug/com.android.messaging.debug.ClassZeroDebugActivity -a com.android.messaging.debug.SHOW_CLASS_ZERO --es body "Fake Class 0 message from adb" --es address "+15551230000"'
 */
class ClassZeroDebugActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UIIntents
            .get()
            .launchClassZeroActivity(
                this,
                buildClassZeroMessageValues(intent = intent),
            )

        finish()
    }
}
