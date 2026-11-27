package com.android.messaging.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.host.DeviceOwnerSmsBlock
import com.android.messaging.ui.host.DeviceOwnerSmsBlockViewModel
import com.android.messaging.util.BugleActivityUtil
import com.android.messaging.util.UiUtils

open class BugleComponentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiUtils.redirectToOnboardingIfNeeded(this)
    }

    override fun onResume() {
        super.onResume()
        BugleActivityUtil.onActivityResume()
    }

    protected fun setAppContent(content: @Composable () -> Unit) {
        setContent {
            AppTheme {
                content()
                DeviceOwnerSmsBlock(model = hiltViewModel<DeviceOwnerSmsBlockViewModel>())
            }
        }
    }
}
