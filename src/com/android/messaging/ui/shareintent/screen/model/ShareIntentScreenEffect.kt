package com.android.messaging.ui.shareintent.screen.model

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.shareintent.model.ShareSendTarget
import kotlinx.collections.immutable.ImmutableSet

internal sealed interface ShareIntentScreenEffect {

    data class OpenConversation(
        val conversationId: String,
    ) : ShareIntentScreenEffect

    data class SendToSelected(
        val targets: ImmutableSet<ShareSendTarget>,
        val draft: ConversationDraft,
    ) : ShareIntentScreenEffect
}
