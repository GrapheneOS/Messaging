package com.android.messaging.ui.shareintent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.shareintent.screen.ShareIntentEffectHandlerImpl
import com.android.messaging.ui.shareintent.screen.ShareIntentScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareIntentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (redirectToSendToIfNeeded()) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val effectHandler = ShareIntentEffectHandlerImpl(
                    activity = this,
                )

                ShareIntentScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }

    private fun redirectToSendToIfNeeded(): Boolean {
        val hasNoDestination = intent.getStringExtra("address").isNullOrEmpty() &&
            intent.getStringExtra(Intent.EXTRA_EMAIL).isNullOrEmpty()

        if (Intent.ACTION_SEND != intent.action || hasNoDestination) {
            return false
        }

        val convIntent = UIIntents.get().getLaunchConversationActivityIntent(this).apply {
            putExtras(intent)
            action = Intent.ACTION_SENDTO
            setDataAndType(intent.data, intent.type)
        }
        startActivity(convIntent)
        finish()
        return true
    }
}
