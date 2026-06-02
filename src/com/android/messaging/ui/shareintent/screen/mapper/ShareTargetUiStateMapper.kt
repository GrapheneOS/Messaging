package com.android.messaging.ui.shareintent.screen.mapper

import androidx.core.net.toUri
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat.LTR
import com.android.messaging.data.shareintent.model.ShareTargetConversation
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.util.AvatarUriUtil
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ShareTargetUiStateMapper {
    fun map(
        conversations: ImmutableList<ShareTargetConversation>,
    ): ImmutableList<ShareTargetUiState>
}

internal class ShareTargetUiStateMapperImpl @Inject constructor() : ShareTargetUiStateMapper {

    override fun map(
        conversations: ImmutableList<ShareTargetConversation>,
    ): ImmutableList<ShareTargetUiState> {
        return conversations
            .map(::toShareTargetUiState)
            .toImmutableList()
    }

    private fun toShareTargetUiState(
        conversation: ShareTargetConversation,
    ): ShareTargetUiState {
        val formatter = BidiFormatter.getInstance()
        val name = conversation.name

        val formattedDestination = conversation.normalizedDestination
            ?.takeUnless { conversation.isGroup }
            ?.let { PhoneUtils.getDefault().formatForDisplay(it) }
        val details = formattedDestination?.takeIf { it.isNotEmpty() && it != name }

        return ShareTargetUiState(
            conversationId = conversation.conversationId,
            displayName = formatter.unicodeWrap(name, LTR),
            details = details?.let { formatter.unicodeWrap(it, LTR) },
            avatarUri = resolveAvatarUri(conversation.icon),
            isGroup = conversation.isGroup,
        )
    }

    private fun resolveAvatarUri(icon: String?): String? {
        val iconUriString = icon?.takeIf(String::isNotBlank) ?: return null
        val iconUri = iconUriString.toUri()

        return when {
            AvatarUriUtil.isAvatarUri(iconUri) -> AvatarUriUtil.getPrimaryUri(iconUri)?.toString()
            else -> iconUriString
        }
    }
}
