package com.android.messaging.ui.shareintent.screen

import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.PendingAttachmentData
import com.android.messaging.domain.shareintent.model.ShareSendTarget
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToTargets
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect
import com.android.messaging.util.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface ShareIntentEffectHandler {
    fun handle(effect: Effect)
}

internal class ShareIntentEffectHandlerImpl(
    private val applicationScope: CoroutineScope,
    private val activity: ComponentActivity,
    private val draft: ConversationDraft?,
    private val sendSharedContentToTargets: SendSharedContentToTargets,
) : ShareIntentEffectHandler {

    private val messageData: MessageData? by lazy {
        draft?.let(::buildSharedMessageData)
    }

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenConversation -> {
                openConversation(effect.conversationId)
            }

            is Effect.SendToSelected -> {
                sendToSelected(effect.targets, effect.draft)
            }
        }
    }

    private fun openConversation(conversationId: String) {
        UIIntents.get().launchConversationActivity(activity, conversationId, messageData)
        activity.finish()
    }

    private fun sendToSelected(
        targets: Set<ShareSendTarget>,
        draft: ConversationDraft,
    ) {
        applicationScope.launch {
            sendSharedContentToTargets(draft, targets)
        }

        UIIntents.get().launchConversationListActivity(activity)
        activity.finish()
    }
}

private fun buildSharedMessageData(draft: ConversationDraft): MessageData {
    return MessageData.createSharedMessage(draft.messageText, draft.subjectText).apply {
        draft.attachments
            .filter { attachment ->
                ContentType.isMediaType(attachment.contentType)
            }
            .forEach { attachment ->
                addPart(
                    PendingAttachmentData.createPendingAttachmentData(
                        attachment.contentType,
                        attachment.contentUri.toUri(),
                    ),
                )
            }
    }
}
