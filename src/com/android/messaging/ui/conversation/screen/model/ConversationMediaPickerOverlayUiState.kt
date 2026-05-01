package com.android.messaging.ui.conversation.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Immutable
internal data class ConversationMediaPickerOverlayUiState(
    val attachments: ImmutableList<ComposerAttachmentUiModel> = persistentListOf(),
    val conversationTitle: String? = null,
    val isSendActionEnabled: Boolean = false,
    val photoPickerSourceContentUriByAttachmentContentUri: ImmutableMap<String, String> =
        persistentMapOf(),
)
