package com.android.messaging.data.conversationsettings.repository

import com.android.messaging.Factory
import com.android.messaging.data.conversation.model.ConversationId
import dagger.hilt.android.EntryPointAccessors

object ConversationSnoozeQuery {

    @JvmStatic
    fun isConversationSnoozed(conversationId: String): Boolean {
        val context = Factory.get().applicationContext
        return EntryPointAccessors
            .fromApplication(context, ConversationNotificationRepository.Provider::class.java)
            .conversationNotificationRepository()
            .isSnoozed(ConversationId(conversationId))
    }
}
