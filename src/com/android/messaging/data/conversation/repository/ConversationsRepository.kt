package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversation.mapper.ConversationMessageDetailsMapper
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.data.conversation.model.message.ConversationMessageDetailsData
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.data.conversation.platform.MessageDetailsPlatformSource
import com.android.messaging.data.conversation.store.ConversationSelfIdStore
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.action.DeleteConversationAction
import com.android.messaging.datamodel.action.DeleteMessageAction
import com.android.messaging.datamodel.action.RedownloadMmsAction
import com.android.messaging.datamodel.action.ResendMessageAction
import com.android.messaging.datamodel.action.UpdateConversationArchiveStatusAction
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.db.ReversedCursor
import com.android.messaging.util.db.ext.getInt
import com.android.messaging.util.db.ext.getLong
import com.android.messaging.util.db.ext.getStringOrEmpty
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal interface ConversationsRepository {
    fun getConversationMetadata(conversationId: String): Flow<ConversationMetadata?>
    suspend fun getConversationMetadataSnapshot(conversationId: String): ConversationMetadata?
    fun getConversationMessages(conversationId: String): Flow<List<ConversationMessageData>>
    suspend fun getConversationSendData(
        conversationId: String,
        requestedSelfParticipantId: String,
    ): ConversationSendData?

    suspend fun getConversationMessage(
        conversationId: String,
        messageId: String,
    ): ConversationMessageData?

    fun deleteMessages(messageIds: Collection<String>)

    fun downloadMessage(messageId: String)

    suspend fun getMessageDetails(
        conversationId: String,
        messageId: String,
    ): ConversationMessageDetails?

    fun resendMessage(messageId: String)

    fun archiveConversation(conversationId: String)

    fun unarchiveConversation(conversationId: String)

    fun deleteConversation(conversationId: String, cutoffTimestamp: Long)

    suspend fun setConversationSelfId(conversationId: String, selfId: String)
}

internal class ConversationsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val messageDetailsMapper: ConversationMessageDetailsMapper,
    private val messageDetailsPlatformSource: MessageDetailsPlatformSource,
    private val conversationSelfIdStore: ConversationSelfIdStore,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : ConversationsRepository {

    override fun getConversationMetadata(conversationId: String): Flow<ConversationMetadata?> {
        val uri = MessagingContentProvider.buildConversationMetadataUri(conversationId)

        return observeUri(uri = uri)
            .flowOn(defaultDispatcher)
            .map {
                queryConversationMetadata(uri = uri)
            }
            .flowOn(messagingDbDispatcher)
    }

    override suspend fun getConversationMetadataSnapshot(
        conversationId: String,
    ): ConversationMetadata? {
        if (conversationId.isBlank()) return null

        val uri = MessagingContentProvider.buildConversationMetadataUri(conversationId)
        return withContext(context = messagingDbDispatcher) {
            queryConversationMetadata(
                uri = uri,
            )
        }
    }

    override fun getConversationMessages(
        conversationId: String,
    ): Flow<List<ConversationMessageData>> {
        val uri = MessagingContentProvider.buildConversationMessagesUri(conversationId)

        return observeUri(uri = uri)
            .flowOn(defaultDispatcher)
            .conflate()
            .map {
                queryConversationMessages(uri = uri)
            }
            .flowOn(messagingDbDispatcher)
    }

    override suspend fun getConversationSendData(
        conversationId: String,
        requestedSelfParticipantId: String,
    ): ConversationSendData? {
        return withContext(context = messagingDbDispatcher) {
            val metadata = when {
                conversationId.isBlank() -> null
                else -> {
                    MessagingContentProvider
                        .buildConversationMetadataUri(conversationId)
                        .let(::queryConversationMetadata)
                }
            }

            metadata?.let { conversationMetadata ->
                val resolvedSelfParticipantId = requestedSelfParticipantId
                    .takeIf { it.isNotBlank() }
                    ?: conversationMetadata.selfParticipantId

                ConversationSendData(
                    metadata = conversationMetadata,
                    participants = queryConversationParticipants(conversationId = conversationId),
                    selfParticipant = queryParticipant(participantId = resolvedSelfParticipantId),
                )
            }
        }
    }

    override suspend fun getConversationMessage(
        conversationId: String,
        messageId: String,
    ): ConversationMessageData? {
        return withContext(context = messagingDbDispatcher) {
            getConversationMessageData(
                conversationId = conversationId,
                messageId = messageId,
            )
        }
    }

    override fun deleteMessages(messageIds: Collection<String>) {
        messageIds
            .asSequence()
            .filter(String::isNotBlank)
            .forEach(DeleteMessageAction::deleteMessage)
    }

    override fun downloadMessage(messageId: String) {
        messageId
            .takeIf { it.isNotBlank() }
            ?.let(RedownloadMmsAction::redownloadMessage)
    }

    override suspend fun getMessageDetails(
        conversationId: String,
        messageId: String,
    ): ConversationMessageDetails? {
        return withContext(context = messagingDbDispatcher) {
            val data = loadMessageDetailsData(
                conversationId = conversationId,
                messageId = messageId,
            ) ?: return@withContext null

            messageDetailsMapper.map(
                data = data,
                activeSubscriptionCount = messageDetailsPlatformSource.activeSubscriptionCount(),
                debug = messageDetailsPlatformSource.loadDebug(data.message),
            )
        }
    }

    override fun resendMessage(messageId: String) {
        messageId
            .takeIf { it.isNotBlank() }
            ?.let(ResendMessageAction::resendMessage)
    }

    override fun archiveConversation(conversationId: String) {
        conversationId
            .takeIf { it.isNotBlank() }
            ?.let(UpdateConversationArchiveStatusAction::archiveConversation)
    }

    override fun unarchiveConversation(conversationId: String) {
        conversationId
            .takeIf { it.isNotBlank() }
            ?.let(UpdateConversationArchiveStatusAction::unarchiveConversation)
    }

    override fun deleteConversation(conversationId: String, cutoffTimestamp: Long) {
        if (conversationId.isBlank()) {
            return
        }

        DeleteConversationAction.deleteConversation(
            conversationId,
            cutoffTimestamp,
        )
    }

    override suspend fun setConversationSelfId(
        conversationId: String,
        selfId: String,
    ) {
        if (conversationId.isBlank() || selfId.isBlank()) return

        withContext(context = messagingDbDispatcher) {
            conversationSelfIdStore.updateSelfId(
                conversationId = conversationId,
                selfId = selfId,
            )
            MessagingContentProvider.notifyConversationListChanged()
            MessagingContentProvider.notifyConversationMetadataChanged(conversationId)
        }
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

    private fun getConversationMessageData(
        conversationId: String,
        messageId: String,
    ): ConversationMessageData? {
        return when {
            conversationId.isBlank() || messageId.isBlank() -> null

            else -> {
                MessagingContentProvider
                    .buildConversationMessagesUri(conversationId)
                    .let(::queryConversationMessages)
                    .firstOrNull { it.messageId == messageId }
            }
        }
    }

    private fun queryConversationMetadata(uri: Uri): ConversationMetadata? {
        return contentResolver
            .query(
                uri,
                ConversationListItemData.PROJECTION,
                null,
                null,
                null,
            )
            ?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                val participantCount = cursor.getInt(ConversationColumns.PARTICIPANT_COUNT)

                val otherParticipant = when {
                    participantCount == 1 -> queryConversationOtherParticipant(uri = uri)
                    else -> null
                }

                val otherParticipantContactLookupKey = otherParticipant
                    ?.lookupKey
                    ?.takeIf { it.isNotBlank() }
                    ?: cursor
                        .getStringOrEmpty(ConversationColumns.PARTICIPANT_LOOKUP_KEY)
                        .takeIf { it.isNotBlank() }

                ConversationMetadata(
                    conversationName = cursor.getStringOrEmpty(ConversationColumns.NAME),
                    selfParticipantId = cursor.getStringOrEmpty(
                        ConversationColumns.CURRENT_SELF_ID,
                    ),
                    isGroupConversation = participantCount > 1,
                    includeEmailAddress = cursor.getInt(
                        ConversationColumns.INCLUDE_EMAIL_ADDRESS,
                    ) == 1,
                    participantCount = participantCount,
                    otherParticipantDisplayDestination = otherParticipant
                        ?.displayDestination
                        ?.takeIf { it.isNotBlank() },
                    otherParticipantNormalizedDestination = cursor
                        .getStringOrEmpty(
                            ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION,
                        )
                        .takeIf { it.isNotBlank() },
                    otherParticipantContactLookupKey = otherParticipantContactLookupKey,
                    otherParticipantPhotoUri = otherParticipant
                        ?.profilePhotoUri
                        ?.takeIf { it.isNotBlank() },
                    isArchived = cursor.getInt(ConversationColumns.ARCHIVE_STATUS) == 1,
                    composerAvailability = ConversationComposerAvailability.Editable,
                    sortTimestamp = cursor.getLong(ConversationColumns.SORT_TIMESTAMP),
                )
            }
    }

    private fun loadMessageDetailsData(
        conversationId: String,
        messageId: String,
    ): ConversationMessageDetailsData? {
        val message = getConversationMessageData(
            conversationId = conversationId,
            messageId = messageId,
        ) ?: return null

        val participants = queryConversationParticipants(
            conversationId = conversationId,
        )
        val selfParticipant = queryParticipant(
            participantId = message.selfParticipantId,
        )

        return ConversationMessageDetailsData(
            message = message,
            participants = participants,
            selfParticipant = selfParticipant,
        )
    }

    private fun queryConversationOtherParticipant(uri: Uri): ParticipantData? {
        val conversationId = uri.lastPathSegment
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val participants = queryConversationParticipants(conversationId = conversationId)
        return participants.getOtherParticipant()
    }

    private fun queryConversationParticipants(
        conversationId: String,
    ): ConversationParticipantsData {
        val uri = MessagingContentProvider.buildConversationParticipantsUri(conversationId)

        return contentResolver
            .query(
                uri,
                ParticipantData.ParticipantsQuery.PROJECTION,
                null,
                null,
                null,
            )
            ?.use { cursor ->
                ConversationParticipantsData().apply {
                    bind(cursor)
                }
            }
            ?: ConversationParticipantsData()
    }

    private fun queryParticipant(
        participantId: String?,
    ): ParticipantData? {
        if (participantId.isNullOrBlank()) {
            return null
        }

        return contentResolver
            .query(
                MessagingContentProvider.PARTICIPANTS_URI,
                ParticipantData.ParticipantsQuery.PROJECTION,
                "${ParticipantColumns._ID} = ?",
                arrayOf(participantId),
                null,
            )
            ?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                ParticipantData.getFromCursor(cursor)
            }
    }

    private fun queryConversationMessages(uri: Uri): List<ConversationMessageData> {
        return contentResolver
            .query(
                uri,
                ConversationMessageData.getProjection(),
                null,
                null,
                null,
            )
            ?.use { rawCursor ->
                val reversedCursor = ReversedCursor(cursor = rawCursor)

                buildList(capacity = rawCursor.count) {
                    while (reversedCursor.moveToNext()) {
                        add(ConversationMessageData().apply { bind(reversedCursor) })
                    }
                }
            }
            ?: emptyList()
    }
}
