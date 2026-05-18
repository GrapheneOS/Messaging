package com.android.messaging.data.conversationsettings.repository

import android.content.ContentResolver
import android.database.ContentObserver
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
import kotlinx.coroutines.withContext

internal interface ConversationSettingsRepository {
    fun observeConversationChanges(conversationId: String): Flow<Unit>
    suspend fun getConversationSettings(conversationId: String): ConversationSettingsData
}

internal class ConversationSettingsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val conversationsRepository: ConversationsRepository,
    private val notificationRepository: ConversationNotificationRepository,
    @param:MessagingDbDispatcher private val messagingDbDispatcher: CoroutineDispatcher,
) : ConversationSettingsRepository {

    override fun observeConversationChanges(
        conversationId: String,
    ): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            val metadataUri = MessagingContentProvider.buildConversationMetadataUri(
                conversationId,
            )
            val participantsUri = MessagingContentProvider.buildConversationParticipantsUri(
                conversationId,
            )
            contentResolver.registerContentObserver(metadataUri, false, observer)
            contentResolver.registerContentObserver(participantsUri, false, observer)
            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }

    override suspend fun getConversationSettings(
        conversationId: String,
    ): ConversationSettingsData {
        val isSnoozed = notificationRepository.isSnoozed(conversationId)
        val isVoiceCapable = PhoneUtils.getDefault().isVoiceCapable
        val metadata = conversationsRepository.getConversationMetadataSnapshot(
            conversationId = conversationId,
        )
        val participants = withContext(context = messagingDbDispatcher) {
            queryOtherParticipants(conversationId)
        }

        return ConversationSettingsData(
            conversationId = conversationId,
            conversationTitle = metadata?.conversationName.orEmpty(),
            isArchived = metadata?.isArchived ?: false,
            isSnoozed = isSnoozed,
            isVoiceCapable = isVoiceCapable,
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
}
