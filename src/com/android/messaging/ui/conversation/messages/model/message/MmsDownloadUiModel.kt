package com.android.messaging.ui.conversation.messages.model.message

import androidx.compose.runtime.Immutable

@Immutable
internal data class MmsDownloadUiModel(
    val state: State,
    val sizeBytes: Long,
    val expiryTimestamp: Long,
    val isSecondaryUser: Boolean,
) {
    enum class State {
        AwaitingManualDownload,
        Downloading,
        DownloadFailed,
        ExpiredOrUnavailable,
    }
}
