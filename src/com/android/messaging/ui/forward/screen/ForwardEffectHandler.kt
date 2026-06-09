package com.android.messaging.ui.forward.screen

import android.app.Activity
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.shareintent.model.ShareSendTarget
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToTargets
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.shareintent.screen.ShareIntentEffectHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect

internal class ForwardEffectHandler(
    private val applicationScope: CoroutineScope,
    private val activity: Activity,
    private val message: MessageData,
    private val sendSharedContentToTargets: SendSharedContentToTargets,
) : ShareIntentEffectHandler {

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
        UIIntents.get().launchConversationActivity(
            activity,
            conversationId,
            message,
        )
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
