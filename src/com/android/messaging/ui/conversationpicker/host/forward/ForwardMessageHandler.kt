package com.android.messaging.ui.conversationpicker.host.forward

import android.app.Activity
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.conversationpicker.model.SendTarget
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationpicker.ConversationPickerEffectHandler
import com.android.messaging.ui.conversationpicker.model.ConversationPickerEffect as Effect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ForwardMessageHandler(
    private val applicationScope: CoroutineScope,
    private val activity: Activity,
    private val message: MessageData,
    private val sendContentToTargets: SendContentToTargets,
) : ConversationPickerEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenConversation -> {
                openConversation(effect.conversationId)
            }

            is Effect.SendToSelected -> {
                sendToSelected(effect.targets, effect.draft)
            }

            is Effect.OpenAttachmentPreview -> Unit
            is Effect.OpenConversationFailed -> Unit
        }
    }

    private fun openConversation(conversationId: String) {
        UIIntents.get().launchConversationActivity(activity, conversationId, message)
        activity.finish()
    }

    private fun sendToSelected(
        targets: Set<SendTarget>,
        draft: ConversationDraft,
    ) {
        applicationScope.launch {
            sendContentToTargets(draft, targets)
        }

        UIIntents.get().launchConversationListActivity(activity)
        activity.finish()
    }
}
