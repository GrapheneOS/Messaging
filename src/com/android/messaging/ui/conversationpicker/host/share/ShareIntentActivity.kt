package com.android.messaging.ui.conversationpicker.host.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.domain.conversationpicker.usecase.BuildMessageDataFromDraft
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.domain.shareintent.usecase.BuildSharedConversationDraft
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationpicker.ConversationPickerScreen
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint
class ShareIntentActivity : ComponentActivity() {

    @Inject
    @ApplicationCoroutineScope
    internal lateinit var applicationScope: CoroutineScope

    @Inject
    internal lateinit var sendContentToTargets: SendContentToTargets

    @Inject
    internal lateinit var buildSharedConversationDraft: BuildSharedConversationDraft

    @Inject
    internal lateinit var buildMessageDataFromDraft: BuildMessageDataFromDraft

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (redirectToSendToIfNeeded()) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                var isDraftLoading by remember { mutableStateOf(true) }
                val conversationDraft by produceState<ConversationDraft?>(
                    initialValue = null,
                ) {
                    value = buildSharedConversationDraft(intent, initialCaller)
                    isDraftLoading = false
                }

                val effectHandler = remember(conversationDraft) {
                    ShareIntentEffectHandler(
                        applicationScope = applicationScope,
                        activity = this,
                        draft = conversationDraft,
                        sendContentToTargets = sendContentToTargets,
                        buildMessageDataFromDraft = buildMessageDataFromDraft,
                    )
                }

                ConversationPickerScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                    allowMultiSelect = true,
                    isInitialDraftLoading = isDraftLoading,
                    initialDraft = conversationDraft,
                )
            }
        }
    }

    private fun redirectToSendToIfNeeded(): Boolean {
        val hasNoDestination = intent.getStringExtra(EXTRA_ADDRESS).isNullOrEmpty() &&
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

    private companion object {
        private const val EXTRA_ADDRESS = "address"
    }
}
