package com.android.messaging.ui.conversationlist.mapper

import androidx.annotation.StringRes
import com.android.messaging.R
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus

@StringRes
internal fun conversationListMmsDownloadTitleResId(
    status: ConversationListMessageStatus,
): Int? {
    return when (status) {
        ConversationListMessageStatus.IncomingAwaitingManualDownload -> {
            R.string.message_title_manual_download
        }

        ConversationListMessageStatus.IncomingDownloading -> {
            R.string.message_title_downloading
        }

        ConversationListMessageStatus.IncomingDownloadFailed,
        ConversationListMessageStatus.IncomingExpiredOrUnavailable,
        -> {
            R.string.message_title_download_failed
        }

        else -> null
    }
}
