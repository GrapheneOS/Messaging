package com.android.messaging.ui.conversationpicker.host.forward

import android.app.Activity
import com.android.messaging.R
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.conversationpicker.model.SendContentResult
import com.android.messaging.domain.conversationpicker.model.SendTarget
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.common.components.attachment.openAttachmentPreview
import com.android.messaging.ui.conversationpicker.ConversationPickerEffectHandler
import com.android.messaging.ui.conversationpicker.model.ConversationPickerEffect as Effect
import com.android.messaging.util.UiUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ForwardMessageEffectHandler(
    private val applicationScope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher,
    private val activity: Activity,
    private val message: MessageData,
    private val sendContentToTargets: SendContentToTargets,
) : ConversationPickerEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenConversation -> {
                openConversation(effect.conversationId)
            }

            is Effect.OpenConversationFailed -> {
                UiUtils.showToastAtBottom(R.string.conversation_picker_open_failed)
            }

            is Effect.SendToSelected -> {
                sendToSelected(effect.targets, effect.draft)
            }

            is Effect.OpenAttachmentPreview -> {
                openPreview(effect.contentUri, effect.contentType)
            }
        }
    }

    private fun openPreview(
        contentUri: String,
        contentType: String,
    ) {
        applicationScope.launch(mainDispatcher) {
            openAttachmentPreview(
                context = activity,
                contentUri = contentUri,
                contentType = contentType,
            )
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
            val result = sendContentToTargets(draft, targets)
            if (result is SendContentResult.Failure) {
                withContext(mainDispatcher) {
                    UiUtils.showToastAtBottom(R.string.send_message_failure)
                }
            }
        }

        UIIntents.get().launchConversationListActivity(activity)
        activity.finish()
    }
}
