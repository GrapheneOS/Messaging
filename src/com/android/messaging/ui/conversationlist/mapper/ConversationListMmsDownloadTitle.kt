package com.android.messaging.ui.conversationlist.mapper

import androidx.annotation.StringRes
import com.android.messaging.R
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus.MmsDownload

@StringRes
internal fun conversationListMmsDownloadTitleResId(
    status: ConversationListMessageStatus,
    isSecondaryUser: Boolean,
): Int? {
    val downloadStatus = status as? MmsDownload ?: return null

    return when {
        isSecondaryUser && downloadStatus.isSecondaryUserDownloadReferral() -> {
            R.string.message_title_download_secondary_user
        }

        else -> downloadStatus.regularTitleResId()
    }
}

@StringRes
private fun MmsDownload.regularTitleResId(): Int {
    return when (this) {
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
    }
}

private fun MmsDownload.isSecondaryUserDownloadReferral(): Boolean {
    return when (this) {
        ConversationListMessageStatus.IncomingAwaitingManualDownload -> true
        ConversationListMessageStatus.IncomingDownloadFailed -> true
        ConversationListMessageStatus.IncomingDownloading -> false
        ConversationListMessageStatus.IncomingExpiredOrUnavailable -> false
    }
}
