package com.android.messaging.data.conversationlist.model

internal sealed interface ConversationListMessageStatus {

    sealed interface Error : ConversationListMessageStatus
    sealed interface MmsDownload : ConversationListMessageStatus

    data object Unknown : ConversationListMessageStatus
    data object Normal : ConversationListMessageStatus
    data object Sending : ConversationListMessageStatus
    data object Draft : ConversationListMessageStatus

    data object IncomingAwaitingManualDownload : MmsDownload
    data object IncomingDownloading : MmsDownload
    data object IncomingDownloadFailed : MmsDownload, Error
    data object IncomingExpiredOrUnavailable : MmsDownload, Error

    data class Failed(
        val rawTelephonyStatus: Int,
    ) : Error
}
