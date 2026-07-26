package com.android.messaging.ui.photoviewer.navigation

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.navigation.ConversationScopedNavKey
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import kotlinx.serialization.Serializable

@Serializable
internal data class PhotoViewerNavKey(
    override val conversationId: ConversationId,
    val launchRequest: PhotoViewerLaunchRequest,
) : ConversationScopedNavKey
