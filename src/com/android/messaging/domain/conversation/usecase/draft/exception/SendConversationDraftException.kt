package com.android.messaging.domain.conversation.usecase.draft.exception

internal sealed class SendConversationDraftException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal class BlankConversationIdException :
    SendConversationDraftException(
        message = "Conversation id must not be blank.",
    )

internal class EmptyConversationDraftException(
    conversationId: String,
) : SendConversationDraftException(
    message = "Draft must contain content before it can be sent " +
        "for conversation $conversationId.",
)

internal class ConversationRecipientsNotLoadedException(
    conversationId: String,
) : SendConversationDraftException(
    message = "Conversation recipients are not loaded for conversation $conversationId.",
)

internal class UnknownConversationRecipientException(
    conversationId: String,
) : SendConversationDraftException(
    message = "Conversation $conversationId contains an unknown sender.",
)

internal class MissingSelfPhoneNumberForGroupMmsException(
    conversationId: String,
    selfSubId: Int,
) : SendConversationDraftException(
    message = "Missing self phone number for group MMS in conversation $conversationId " +
        "on subId $selfSubId.",
)

internal class ConversationSimNotReadyException(
    conversationId: String,
    selfSubId: Int,
    cause: Throwable,
) : SendConversationDraftException(
    message = "SIM is not ready for conversation $conversationId on subId $selfSubId.",
    cause = cause,
)

internal class TooManyVideoAttachmentsException(
    conversationId: String,
    videoAttachmentCount: Int,
) : SendConversationDraftException(
    message = "Draft for conversation $conversationId has $videoAttachmentCount video " +
        "attachments.",
)

internal class DraftDispatchFailedException(
    conversationId: String,
    cause: Throwable,
) : SendConversationDraftException(
    message = "Failed to enqueue outgoing draft for conversation $conversationId.",
    cause = cause,
)
