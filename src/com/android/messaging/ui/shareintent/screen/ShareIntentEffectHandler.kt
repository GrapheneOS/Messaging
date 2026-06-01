package com.android.messaging.ui.shareintent.screen

import androidx.activity.ComponentActivity
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.domain.shareintent.usecase.SendSharedContentToConversations
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.shareintent.screen.model.ShareIntentScreenEffect as Effect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal interface ShareIntentEffectHandler {
    fun handle(effect: Effect)
}

internal class ShareIntentEffectHandlerImpl(
    private val applicationScope: CoroutineScope,
    private val activity: ComponentActivity,
    private val draft: MessageData?,
    private val sendSharedContentToConversations: SendSharedContentToConversations,
) : ShareIntentEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenConversation -> {
                openConversation(effect.conversationId)
            }

            is Effect.CreateNewConversation -> {
                createNewConversation()
            }

            is Effect.SendToSelected -> {
                sendToSelected(effect.conversationIds)
            }
        }
    }

    private fun openConversation(conversationId: String) {
        UIIntents.get().launchConversationActivity(activity, conversationId, draft)
        activity.finish()
    }

    private fun createNewConversation() {
        UIIntents.get().launchCreateNewConversationActivity(activity, draft)
        activity.finish()
    }

    private fun sendToSelected(conversationIds: Set<String>) {
        val intent = activity.intent

        applicationScope.launch {
            sendSharedContentToConversations(intent, conversationIds)
        }

        UIIntents.get().launchConversationListActivity(activity)
        activity.finish()
    }
}
