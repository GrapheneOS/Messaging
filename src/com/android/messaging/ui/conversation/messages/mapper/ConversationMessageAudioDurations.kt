package com.android.messaging.ui.conversation.messages.mapper

import com.android.messaging.domain.media.usecase.ResolveAudioDurationMillis
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal suspend fun withAudioDurations(
    messages: ImmutableList<ConversationMessageUiModel>,
    resolveAudioDurationMillis: ResolveAudioDurationMillis,
): ImmutableList<ConversationMessageUiModel> {
    val audioContentUris = audioContentUris(messages = messages)

    if (audioContentUris.isEmpty()) {
        return messages
    }

    val durationsByContentUri = resolveDurations(
        contentUris = audioContentUris,
        resolveAudioDurationMillis = resolveAudioDurationMillis,
    )

    return messages
        .map { message ->
            message.withDurationsFrom(durationsByContentUri = durationsByContentUri)
        }
        .toImmutableList()
}

private fun audioContentUris(messages: List<ConversationMessageUiModel>): Set<String> {
    return messages
        .asSequence()
        .flatMap(ConversationMessageUiModel::parts)
        .filterIsInstance<ConversationMessagePartUiModel.Attachment.Audio>()
        .mapNotNullTo(mutableSetOf()) { audioPart -> audioPart.contentUri?.toString() }
}

private suspend fun resolveDurations(
    contentUris: Set<String>,
    resolveAudioDurationMillis: ResolveAudioDurationMillis,
): Map<String, Long> {
    return coroutineScope {
        contentUris
            .map { contentUri ->
                async { contentUri to resolveAudioDurationMillis(contentUri) }
            }
            .awaitAll()
            .toMap()
    }
}

private fun ConversationMessageUiModel.withDurationsFrom(
    durationsByContentUri: Map<String, Long>,
): ConversationMessageUiModel {
    val updatedParts = parts.map { part ->
        part.withDurationFrom(durationsByContentUri = durationsByContentUri)
    }

    return when (updatedParts) {
        parts -> this
        else -> copy(parts = updatedParts.toImmutableList())
    }
}

private fun ConversationMessagePartUiModel.withDurationFrom(
    durationsByContentUri: Map<String, Long>,
): ConversationMessagePartUiModel {
    return when (this) {
        is ConversationMessagePartUiModel.Attachment.Audio -> {
            copy(
                durationMillis = contentUri
                    ?.toString()
                    ?.let(durationsByContentUri::get)
                    ?: durationMillis,
            )
        }

        else -> this
    }
}
