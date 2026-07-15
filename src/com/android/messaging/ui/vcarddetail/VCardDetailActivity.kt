package com.android.messaging.ui.vcarddetail

import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.vcarddetail.screen.VCardDetailEffectHandlerImpl
import com.android.messaging.ui.vcarddetail.screen.VCardDetailScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VCardDetailActivity : BugleComponentActivity() {

    @Inject
    internal lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val effectHandler = remember {
                    VCardDetailEffectHandlerImpl(
                        activity = this,
                        clipboardManager = clipboardManager,
                    )
                }

                VCardDetailScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
