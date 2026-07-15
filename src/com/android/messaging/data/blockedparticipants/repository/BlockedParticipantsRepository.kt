package com.android.messaging.data.blockedparticipants.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.blockedparticipants.model.BlockedDirectChat
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction.UpdateDestinationBlockedActionListener
import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction.updateDestinationBlocked
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.di.core.MessagingDbDispatcher
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal interface BlockedParticipantsRepository {
    fun observeBlockedParticipants(): Flow<ImmutableList<BlockedDirectChat>>

    suspend fun setDestinationBlocked(
        destination: String,
        conversationId: String?,
        isBlocked: Boolean,
    ): Boolean
}

internal class BlockedParticipantsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:MessagingDbDispatcher private val messagingDbDispatcher: CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : BlockedParticipantsRepository {

    private val dataModel = DataModel.get()

    override fun observeBlockedParticipants(): Flow<ImmutableList<BlockedDirectChat>> {
        val uris = listOf(
            MessagingContentProvider.PARTICIPANTS_URI,
            MessagingContentProvider.CONVERSATIONS_URI,
        )

        return observeUris(uris)
            .map { queryBlockedDirectChats() }
            .flowOn(messagingDbDispatcher)
    }

    override suspend fun setDestinationBlocked(
        destination: String,
        conversationId: String?,
        isBlocked: Boolean,
    ): Boolean {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return false

        return withContext(mainDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = UpdateDestinationBlockedActionListener { _, success, _, _ ->
                    if (continuation.isActive) {
                        continuation.resume(success)
                    }
                }

                val actionMonitor = updateDestinationBlocked(
                    resolvedDestination,
                    isBlocked,
                    conversationId?.takeIf(String::isNotBlank),
                    listener,
                )

                continuation.invokeOnCancellation {
                    actionMonitor?.unregister()
                }
            }
        }
    }

    // Returns only blocked participants that have an existing 1 on 1 conversation.
    // Product behavior: after the user deletes a blocked chat the row should disappear
    // from this screen. The participant stays blocked - if a new chat is started later,
    // they reappear here. Participants blocked without ever having a direct chat are
    // intentionally not shown.
    private fun queryBlockedDirectChats(): ImmutableList<BlockedDirectChat> {
        val otherDestination = ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION
        val selection = "${ParticipantColumns.BLOCKED}=1 " +
            "AND ${ParticipantColumns.NORMALIZED_DESTINATION} IN (" +
            "SELECT $otherDestination " +
            "FROM ${DatabaseHelper.CONVERSATIONS_TABLE} " +
            "WHERE $otherDestination IS NOT NULL)"

        val cursor = contentResolver.query(
            MessagingContentProvider.PARTICIPANTS_URI,
            ParticipantData.ParticipantsQuery.PROJECTION,
            selection,
            null,
            null,
        ) ?: return persistentListOf()

        val participants = cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(ParticipantData.getFromCursor(it))
                }
            }
        }

        return participants
            .mapNotNull { participant ->
                val destination = participant.normalizedDestination?.takeIf(String::isNotEmpty)
                    ?: return@mapNotNull null
                val conversationId = BugleDatabaseOperations
                    .getConversationFromOtherParticipantDestination(
                        dataModel.database,
                        destination,
                    )
                    ?: return@mapNotNull null

                BlockedDirectChat(
                    participant = participant,
                    conversationId = conversationId,
                )
            }
            .toImmutableList()
    }

    private fun observeUris(uris: List<Uri>): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }

            uris.forEach { uri ->
                contentResolver.registerContentObserver(uri, true, observer)
            }

            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }
}
