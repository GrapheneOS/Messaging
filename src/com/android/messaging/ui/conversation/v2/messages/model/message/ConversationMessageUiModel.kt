package com.android.messaging.ui.conversation.v2.messages.model.message

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
@Immutable
internal data class ConversationMessageUiModel(
    val messageId: String,
    val conversationId: String,
    val text: String?,
    val parts: List<ConversationMessagePartUiModel>,
    val sentTimestamp: Long,
    val receivedTimestamp: Long,
    val displayTimestamp: Long,
    val status: Status,
    val isIncoming: Boolean,
    val senderDisplayName: String?,
    val senderAvatarUri: Uri?,
    val senderContactLookupKey: String?,
    val canClusterWithPrevious: Boolean,
    val canClusterWithNext: Boolean,
    val mmsSubject: String?,
    val protocol: Protocol,
) {

    @Stable
    sealed interface Status {
        data object Unknown : Status

        sealed interface Outgoing : Status {
            data object Complete : Outgoing
            data object Delivered : Outgoing
            data object Draft : Outgoing
            data object YetToSend : Outgoing
            data object Sending : Outgoing
            data object Resending : Outgoing
            data object AwaitingRetry : Outgoing
            data object Failed : Outgoing
            data object FailedEmergencyNumber : Outgoing
        }

        sealed interface Incoming : Status {
            data object Complete : Incoming
            data object YetToManualDownload : Incoming
            data object RetryingManualDownload : Incoming
            data object ManualDownloading : Incoming
            data object RetryingAutoDownload : Incoming
            data object AutoDownloading : Incoming
            data object DownloadFailed : Incoming
            data object ExpiredOrNotAvailable : Incoming
        }
    }

    enum class Protocol {
        UNKNOWN,
        SMS,
        MMS,
        MMS_PUSH_NOTIFICATION,
    }
}
