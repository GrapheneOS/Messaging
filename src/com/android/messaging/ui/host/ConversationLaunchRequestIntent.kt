package com.android.messaging.ui.host

import android.content.Intent
import android.text.TextUtils
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest

internal fun Intent.hasConversationLaunchPayload(): Boolean {
    return hasExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID) ||
        hasExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA) ||
        hasExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI)
}

internal fun Intent.toConversationLaunchRequest(
    launchGeneration: Int,
    isLaunchedFromBubble: Boolean,
): ConversationEntryLaunchRequest {
    val launchRequest = ConversationEntryLaunchRequest(
        launchGeneration = launchGeneration,
        conversationId = getStringExtra(
            UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID
        ).let(ConversationId::fromOrNull),
        draftData = getParcelableExtra(
            UIIntents.UI_INTENT_EXTRA_DRAFT_DATA,
            MessageData::class.java,
        ),
        startupAttachmentUri = getStringExtra(
            UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI
        )?.takeUnless(TextUtils::isEmpty),
        startupAttachmentType = getStringExtra(
            UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE
        )?.takeUnless(TextUtils::isEmpty),
        messagePosition = getIntExtra(
            UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION,
            -1,
        ).takeIf { position -> position >= 0 },
        isLaunchedFromBubble = isLaunchedFromBubble,
    )

    removeExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA)
    removeExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION)

    return launchRequest
}
