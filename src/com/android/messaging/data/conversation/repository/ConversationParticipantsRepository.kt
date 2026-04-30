package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.IoDispatcher
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface ConversationParticipantsRepository {
    fun getParticipants(
        conversationId: String,
    ): Flow<ImmutableList<ConversationRecipient>>
}

internal class ConversationParticipantsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ConversationParticipantsRepository {

    override fun getParticipants(
        conversationId: String,
    ): Flow<ImmutableList<ConversationRecipient>> {
        val uri = MessagingContentProvider.buildConversationParticipantsUri(conversationId)

        return observeUri(uri = uri)
            .conflate()
            .map {
                queryParticipants(uri = uri)
            }
            .flowOn(ioDispatcher)
    }

    private fun observeUri(uri: Uri): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            contentResolver.registerContentObserver(uri, true, observer)

            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    private fun queryParticipants(
        uri: Uri,
    ): ImmutableList<ConversationRecipient> {
        return contentResolver
            .query(
                uri,
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )
            ?.use { cursor ->
                val participants = persistentListOf<ConversationRecipient>().builder()
                val seenDestinations = LinkedHashSet<String>()

                while (cursor.moveToNext()) {
                    val participant = ParticipantData.getFromCursor(cursor)
                    val recipient = mapParticipant(participant = participant)

                    if (recipient != null && seenDestinations.add(recipient.destination)) {
                        participants.add(recipient)
                    }
                }

                participants.build()
            }
            ?: persistentListOf()
    }

    private fun mapParticipant(participant: ParticipantData): ConversationRecipient? {
        val destination = participantDestination(participant = participant) ?: return null
        val displayName = participant.getDisplayName(true)

        return ConversationRecipient(
            id = participant.id,
            displayName = displayName,
            destination = destination,
            photoUri = participant.profilePhotoUri,
            secondaryText = participant.displayDestination
                ?.takeIf { it.isNotBlank() }
                ?.takeIf { it != displayName },
        )
    }

    private fun participantDestination(participant: ParticipantData): String? {
        return when {
            participant.isSelf -> null

            else -> {
                participant.sendDestination
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
        }
    }
}
