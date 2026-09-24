package com.android.messaging.ui.conversation.screen.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet

@Immutable
internal data class ConversationMessageSelectionUiState(
    val selectedMessageIds: ImmutableSet<MessageId> = persistentSetOf(),
    val availableActions: ImmutableSet<ConversationMessageAction> = persistentSetOf(),
    val deleteConfirmation: ConversationMessageDeleteConfirmationUiState? = null,
) {
    val isSelectionMode: Boolean
        get() = selectedMessageIds.isNotEmpty()

    val isMultiSelect: Boolean
        get() = selectedMessageIds.size > 1

    val selectedMessageCount: Int
        get() = selectedMessageIds.size
}

@Immutable
internal data class ConversationMessageDeleteConfirmationUiState(
    val messageIds: ImmutableSet<MessageId> = persistentSetOf(),
)

internal enum class ConversationMessageAction(
    @param:StringRes val labelRes: Int,
) {
    Copy(labelRes = R.string.message_context_menu_copy_text),
    Delete(labelRes = R.string.action_delete_message),
    Details(labelRes = R.string.message_context_menu_view_details),
    Download(labelRes = R.string.action_download),
    Forward(labelRes = R.string.message_context_menu_forward_message),
    Resend(labelRes = R.string.action_send),
    SaveAttachment(labelRes = R.string.action_save_attachment),
    Share(labelRes = R.string.action_share),
}

internal fun ConversationMessageUiModel.availableMessageActions():
    ImmutableSet<ConversationMessageAction> {
    val actions = LinkedHashSet<ConversationMessageAction>()

    if (canDownloadMessage) {
        actions += ConversationMessageAction.Download
    }

    if (canResendMessage) {
        actions += ConversationMessageAction.Resend
    }

    if (canCopyMessageToClipboard) {
        actions += ConversationMessageAction.Copy
    }

    actions += ConversationMessageAction.Delete

    if (canForwardMessage) {
        actions += ConversationMessageAction.Share
        actions += ConversationMessageAction.Forward
    }

    if (canSaveAttachments) {
        actions += ConversationMessageAction.SaveAttachment
    }

    actions += ConversationMessageAction.Details

    return actions.toImmutableSet()
}
