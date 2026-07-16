package com.android.messaging.domain.conversation.usecase.draft.exception

import com.android.messaging.data.conversation.model.ConversationId

internal sealed class SendConversationDraftException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal class BlankConversationIdException :
    SendConversationDraftException(
        message = "Conversation id must not be blank.",
    )

internal class EmptyConversationDraftException(
    conversationId: ConversationId,
) : SendConversationDraftException(
    message = "Draft must contain content before it can be sent " +
        "for conversation ${conversationId.value}.",
)

internal class ConversationRecipientsNotLoadedException(
    conversationId: ConversationId,
) : SendConversationDraftException(
    message = "Conversation recipients are not loaded for conversation ${conversationId.value}.",
)

internal class UnknownConversationRecipientException(
    conversationId: ConversationId,
) : SendConversationDraftException(
    message = "Conversation ${conversationId.value} contains an unknown sender.",
)

internal class MissingSelfPhoneNumberForGroupMmsException(
    conversationId: ConversationId,
    selfSubId: Int,
) : SendConversationDraftException(
    message = "Missing self phone number for group MMS in conversation ${conversationId.value} " +
        "on subId $selfSubId.",
)

internal class ConversationSimNotReadyException(
    conversationId: ConversationId,
    selfSubId: Int,
    cause: Throwable,
) : SendConversationDraftException(
    message = "SIM is not ready for conversation ${conversationId.value} on subId $selfSubId.",
    cause = cause,
)

internal class TooManyVideoAttachmentsException(
    conversationId: ConversationId,
    videoAttachmentCount: Int,
) : SendConversationDraftException(
    message = "Draft for conversation ${conversationId.value} has $videoAttachmentCount video " +
        "attachments.",
)

internal class MessageLimitExceededException(
    conversationId: ConversationId,
) : SendConversationDraftException(
    message = "Draft for conversation ${conversationId.value} exceeds the MMS message limit.",
)

internal class DraftDispatchFailedException(
    conversationId: ConversationId,
    cause: Throwable,
) : SendConversationDraftException(
    message = "Failed to enqueue outgoing draft for conversation ${conversationId.value}.",
    cause = cause,
)
