package com.android.messaging.ui.conversation.messages.ui.message

import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel

internal fun resolveConversationMessageSimDisplayName(
    message: ConversationMessageUiModel,
    messageBelow: ConversationMessageUiModel?,
    simDisplayNameByParticipantId: Map<ParticipantId, String>,
): String? {
    val selfParticipantId = message.selfParticipantId
    val displayName = selfParticipantId?.let(simDisplayNameByParticipantId::get)

    val isLastInSimRun = messageBelow == null ||
        messageBelow.isIncoming != message.isIncoming ||
        messageBelow.selfParticipantId != selfParticipantId

    return when {
        simDisplayNameByParticipantId.size <= 1 -> null
        message.mmsDownload != null -> displayName
        !isLastInSimRun -> null
        else -> displayName
    }
}
