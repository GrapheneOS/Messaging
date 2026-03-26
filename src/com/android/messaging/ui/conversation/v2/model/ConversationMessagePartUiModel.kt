package com.android.messaging.ui.conversation.v2.messages.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
internal data class ConversationMessagePartUiModel(
    val contentType: String,
    val text: String?,
    val contentUri: Uri?,
    val width: Int,
    val height: Int,
)
