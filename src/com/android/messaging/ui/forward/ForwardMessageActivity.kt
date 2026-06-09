package com.android.messaging.ui.forward

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.domain.forward.usecase.BuildForwardConversationDraft
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToTargets
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.forward.screen.ForwardEffectHandler
import com.android.messaging.ui.shareintent.screen.ShareIntentScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint
class ForwardMessageActivity : ComponentActivity() {

    @Inject
    @ApplicationCoroutineScope
    internal lateinit var applicationScope: CoroutineScope

    @Inject
    internal lateinit var sendSharedContentToTargets: SendSharedContentToTargets

    @Inject
    internal lateinit var buildForwardConversationDraft: BuildForwardConversationDraft

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = IntentCompat.getParcelableExtra(
            intent,
            UIIntents.UI_INTENT_EXTRA_DRAFT_DATA,
            MessageData::class.java,
        )

        if (message == null) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val draft = remember(message) {
                    buildForwardConversationDraft(message)
                }

                val effectHandler = remember(message) {
                    ForwardEffectHandler(
                        applicationScope = applicationScope,
                        activity = this,
                        message = message,
                        sendSharedContentToTargets = sendSharedContentToTargets,
                    )
                }

                ShareIntentScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                    allowMultiSelect = true,
                    isInitialDraftLoading = false,
                    initialDraft = draft,
                )
            }
        }
    }
}
