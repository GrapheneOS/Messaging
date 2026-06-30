package com.android.messaging.data.conversationlist.model

internal sealed interface ConversationListMessageStatus {

    sealed interface Error : ConversationListMessageStatus

    data object Unknown : ConversationListMessageStatus
    data object Normal : ConversationListMessageStatus
    data object Sending : ConversationListMessageStatus
    data object Draft : ConversationListMessageStatus

    data object IncomingAwaitingManualDownload : ConversationListMessageStatus
    data object IncomingDownloading : ConversationListMessageStatus

    data class Failed(
        val rawTelephonyStatus: Int,
    ) : Error

    data object IncomingDownloadFailed : Error
    data object IncomingExpiredOrUnavailable : Error
}
