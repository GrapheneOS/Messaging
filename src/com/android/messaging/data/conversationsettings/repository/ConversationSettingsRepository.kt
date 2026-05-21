@file:OptIn(ExperimentalCoroutinesApi::class)

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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

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
        val uris = listOf(
            MessagingContentProvider.buildConversationMetadataUri(conversationId),
            MessagingContentProvider.buildConversationParticipantsUri(conversationId),
        )

        return refreshTriggers(conversationId, uris)
            .map { loadConversationSettings(conversationId) }
            .flowOn(messagingDbDispatcher)
    }

    private fun refreshTriggers(
        conversationId: String,
        uris: List<Uri>,
    ): Flow<Unit> {
        return observeUris(uris).flatMapLatest {
            val immediate = flowOf(Unit)
            val snoozeExpired = snoozeExpiry(conversationId)
            merge(immediate, snoozeExpired)
        }
    }

    private fun snoozeExpiry(
        conversationId: String,
    ): Flow<Unit> {
        return flow {
            val snoozeUntilMillis = notificationRepository.getSnoozeUntilMillis(conversationId)
            if (snoozeUntilMillis == Long.MAX_VALUE) return@flow

            val remaining = snoozeUntilMillis - System.currentTimeMillis()
            if (remaining <= 0L) return@flow

            delay(remaining)
            emit(Unit)
        }
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
