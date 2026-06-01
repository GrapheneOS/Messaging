package com.android.messaging.ui.shareintent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.domain.shareintent.usecase.BuildSharedDraftMessage
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToConversations
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.shareintent.screen.ShareIntentEffectHandlerImpl
import com.android.messaging.ui.shareintent.screen.ShareIntentScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint
class ShareIntentActivity : ComponentActivity() {

    @Inject
    @ApplicationCoroutineScope
    internal lateinit var applicationScope: CoroutineScope

    @Inject
    internal lateinit var buildSharedDraftMessage: BuildSharedDraftMessage

    @Inject
    internal lateinit var sendSharedContentToConversations: SendSharedContentToConversations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (redirectToSendToIfNeeded()) {
            return
        }

        val draft = buildSharedDraftMessage(intent)

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val effectHandler = remember(draft) {
                    ShareIntentEffectHandlerImpl(
                        applicationScope = applicationScope,
                        activity = this,
                        draft = draft,
                        sendSharedContentToConversations = sendSharedContentToConversations,
                    )
                }

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
