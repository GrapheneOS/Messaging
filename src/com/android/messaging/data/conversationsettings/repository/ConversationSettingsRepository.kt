package com.android.messaging.data.conversationsettings.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversation.repository.ConversationsRepository
import com.android.messaging.data.conversationsettings.model.ConversationSettingsData
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface ConversationSettingsRepository {
    fun getConversationSettings(conversationId: String): Flow<ConversationSettingsData>
}

internal class ConversationSettingsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val conversationsRepository: ConversationsRepository,
    private val notificationRepository: ConversationNotificationRepository,
    @param:MessagingDbDispatcher private val messagingDbDispatcher: CoroutineDispatcher,
) : ConversationSettingsRepository {

    override fun getConversationSettings(
        conversationId: String,
    ): Flow<ConversationSettingsData> {
        val metadataUri = MessagingContentProvider.buildConversationMetadataUri(
            conversationId
        )
        val participantsUri = MessagingContentProvider.buildConversationParticipantsUri(
            conversationId
        )

        return observeUris(uris = listOf(metadataUri, participantsUri))
            .map { loadConversationSettings(conversationId = conversationId) }
            .flowOn(messagingDbDispatcher)
    }

    private suspend fun loadConversationSettings(
        conversationId: String,
    ): ConversationSettingsData {
        val phoneUtils = PhoneUtils.getDefault()
        val participants = queryOtherParticipants(conversationId)
        val metadata = conversationsRepository.getConversationMetadataSnapshot(
            conversationId = conversationId,
        )

        return ConversationSettingsData(
            conversationId = conversationId,
            conversationTitle = metadata?.conversationName.orEmpty(),
            isArchived = metadata?.isArchived ?: false,
            isSnoozed = notificationRepository.isSnoozed(conversationId),
            isVoiceCapable = phoneUtils.isVoiceCapable,
            participants = participants.toImmutableList(),
            dbSelfParticipantId = metadata?.selfParticipantId.orEmpty(),
        )
    }

    private fun queryOtherParticipants(
        conversationId: String,
    ): List<ParticipantData> {
        val participantsData = ConversationParticipantsData().apply {
            contentResolver.query(
                MessagingContentProvider.buildConversationParticipantsUri(conversationId),
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )?.use { bind(it) }
        }

        return participantsData.filter { !it.isSelf }
    }

    private fun observeUris(uris: List<Uri>): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            uris.forEach { uri ->
                contentResolver.registerContentObserver(uri, false, observer)
            }
            trySend(Unit)
            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }
}
