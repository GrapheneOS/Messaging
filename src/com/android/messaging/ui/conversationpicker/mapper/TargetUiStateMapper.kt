package com.android.messaging.ui.conversationpicker.mapper

import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.conversationpicker.model.TargetConversation
import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.ui.conversationpicker.formatter.targetDetailsTextOrNull
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface TargetUiStateMapper {
    fun map(
        conversations: ImmutableList<TargetConversation>,
    ): ImmutableList<TargetUiState>
}

internal class TargetUiStateMapperImpl @Inject constructor(
    private val contactDestinationFormatter: ContactDestinationFormatter,
    private val phoneNumberFormatter: PhoneNumberFormatter,
    private val resolveAvatarUri: ResolveAvatarUri,
) : TargetUiStateMapper {

    override fun map(
        conversations: ImmutableList<TargetConversation>,
    ): ImmutableList<TargetUiState> {
        return conversations
            .map(::toTargetUiState)
            .toImmutableList()
    }

    private fun toTargetUiState(
        conversation: TargetConversation,
    ): TargetUiState {
        val targetDisplayName = conversation.name

        val otherParticipantDestination = conversation.normalizedDestination
            ?.takeUnless { conversation.isGroup }

        val formattedDestination = otherParticipantDestination
            ?.let(phoneNumberFormatter::formatForDisplay)

        val canonicalDestination = otherParticipantDestination
            ?.let(contactDestinationFormatter::canonicalize)
            ?.takeIf { it.isNotEmpty() }

        return TargetUiState.Conversation(
            conversationId = conversation.conversationId,
            normalizedDestination = canonicalDestination,
            displayName = targetDisplayName,
            details = targetDetailsTextOrNull(
                displayName = targetDisplayName,
                value = formattedDestination,
            ),
            avatarUri = resolveAvatarUri(conversation.icon),
            isGroup = conversation.isGroup,
        )
    }
}
