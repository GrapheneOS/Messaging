package com.android.messaging.data.conversation.notification

import com.android.messaging.domain.conversation.usecase.notification.IsConversationSnoozed
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConversationSnoozeChecker @Inject internal constructor(
    private val isConversationSnoozed: IsConversationSnoozed,
) {

    fun isSnoozed(conversationId: String): Boolean {
        return isConversationSnoozed(conversationId)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun conversationSnoozeChecker(): ConversationSnoozeChecker
    }
}
