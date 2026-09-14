package com.android.messaging.domain.conversation.usecase.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.datamodel.MessageTextStats
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.sms.MmsUtils
import javax.inject.Inject

internal fun interface GetConversationDraftSendProtocol {
    operator fun invoke(
        draft: ConversationDraft,
        sendData: ConversationSendData,
    ): ConversationDraftSendProtocol
}

internal class GetConversationDraftSendProtocolImpl @Inject constructor() :
    GetConversationDraftSendProtocol {

    override operator fun invoke(
        draft: ConversationDraft,
        sendData: ConversationSendData,
    ): ConversationDraftSendProtocol {
        return when {
            shouldSendAsMms(
                draft = draft,
                sendData = sendData,
            ) -> ConversationDraftSendProtocol.MMS

            else -> ConversationDraftSendProtocol.SMS
        }
    }

    private fun shouldSendAsMms(
        draft: ConversationDraft,
        sendData: ConversationSendData,
    ): Boolean {
        val selfSubId = sendData.selfSubId
        val conversationMetadata = sendData.metadata

        val groupConversationRequiresMms = conversationMetadata.isGroupConversation &&
            MmsUtils.groupMmsEnabled(selfSubId.value)

        val emailAddressRequiresMms = MmsSmsUtils.getRequireMmsForEmailAddress(
            conversationMetadata.includeEmailAddress,
            selfSubId.value,
        )

        return when {
            draft.attachments.isNotEmpty() -> true
            draft.subjectText.isNotBlank() -> true
            groupConversationRequiresMms -> true
            emailAddressRequiresMms -> true

            else -> {
                messageLengthRequiresMms(
                    messageText = draft.messageText,
                    selfSubId = selfSubId,
                )
            }
        }
    }

    private fun messageLengthRequiresMms(
        messageText: String,
        selfSubId: SubId,
    ): Boolean {
        return MessageTextStats()
            .apply {
                updateMessageTextStats(selfSubId.value, messageText)
            }
            .messageLengthRequiresMms
    }
}
