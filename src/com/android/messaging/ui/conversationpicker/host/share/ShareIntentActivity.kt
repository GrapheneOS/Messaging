package com.android.messaging.ui.conversationpicker.host.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversationpicker.usecase.BuildMessageDataFromDraft
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.domain.shareintent.model.SharedConversationDraftResult
import com.android.messaging.domain.shareintent.usecase.BuildSharedConversationDraft
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationpicker.ConversationPickerScreen
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint
class ShareIntentActivity : BugleComponentActivity() {

    @Inject
    @ApplicationCoroutineScope
    internal lateinit var applicationScope: CoroutineScope

    @Inject
    @MainDispatcher
    internal lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    internal lateinit var sendContentToTargets: SendContentToTargets

    @Inject
    internal lateinit var buildSharedConversationDraft: BuildSharedConversationDraft

    @Inject
    internal lateinit var buildMessageDataFromDraft: BuildMessageDataFromDraft

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        if (redirectToSendToIfNeeded()) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val shareDraft = rememberShareDraftState()
                val effectHandler = rememberShareIntentEffectHandler(shareDraft.draft)

                LaunchedEffect(shareDraft) {
                    if (shareDraft.isLoading) {
                        return@LaunchedEffect
                    }

                    when {
                        shareDraft.draft == null -> {
                            effectHandler.showNoShareableContentNotice()
                            finish()
                        }

                        shareDraft.hasDroppedContent -> {
                            effectHandler.showDroppedContentNotice()
                        }
                    }
                }

                ConversationPickerScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                    allowMultiSelect = true,
                    labels = conversationPickerLabels(),
                    isInitialDraftLoading = shareDraft.isLoading,
                    initialDraft = shareDraft.draft,
                )
            }
        }
    }

    @Composable
    private fun rememberShareDraftState(): ShareDraftState {
        val draftResult by produceState<SharedConversationDraftResult?>(
            initialValue = null,
        ) {
            value = buildSharedConversationDraft(intent)
        }

        return ShareDraftState(
            draft = draftResult?.draft,
            isLoading = draftResult == null,
            hasDroppedContent = draftResult?.hasDroppedContent == true,
        )
    }

    @Composable
    private fun rememberShareIntentEffectHandler(
        draft: ConversationDraft?,
    ): ShareIntentEffectHandler {
        return remember(draft) {
            ShareIntentEffectHandler(
                applicationScope = applicationScope,
                mainDispatcher = mainDispatcher,
                activity = this,
                draft = draft,
                sendContentToTargets = sendContentToTargets,
                buildMessageDataFromDraft = buildMessageDataFromDraft,
            )
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

    private fun conversationPickerLabels(): ConversationPickerLabels {
        return when (intent.getStringExtra(EXTRA_CONVERSATION_PICKER_LABELS)) {
            LABELS_FORWARD -> ConversationPickerLabels.Forward
            else -> ConversationPickerLabels.Share
        }
    }

    private data class ShareDraftState(
        val draft: ConversationDraft?,
        val isLoading: Boolean,
        val hasDroppedContent: Boolean,
    )

    companion object {
        internal fun createForwardIntent(
            context: Context,
            uri: Uri,
            contentType: String,
        ): Intent {
            return Intent(context, ShareIntentActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = contentType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(EXTRA_CONVERSATION_PICKER_LABELS, LABELS_FORWARD)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        private const val EXTRA_ADDRESS = "address"
        private const val EXTRA_CONVERSATION_PICKER_LABELS =
            "com.android.messaging.extra.CONVERSATION_PICKER_LABELS"
        private const val LABELS_FORWARD = "forward"
    }
}
