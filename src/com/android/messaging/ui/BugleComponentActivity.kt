package com.android.messaging.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.android.messaging.util.BugleActivityUtil
import com.android.messaging.util.UiUtils

open class BugleComponentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiUtils.redirectToPermissionCheckIfNeeded(this)
    }

    override fun onResume() {
        super.onResume()
        BugleActivityUtil.onActivityResume(this, this)
    }
}
