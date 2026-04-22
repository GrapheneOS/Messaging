package com.android.messaging.data.conversation.model.draft

// TODO: Probably should be sealed interface and mapped to types on this stage
internal data class ConversationDraftAttachment(
    val contentType: String,
    val contentUri: String,
    val captionText: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: Long? = null,
)
