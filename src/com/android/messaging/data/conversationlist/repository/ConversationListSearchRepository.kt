package com.android.messaging.data.conversationlist.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMode
import com.android.messaging.data.conversationlist.model.SearchableConversation
import com.android.messaging.data.conversationlist.model.SearchableParticipant
import com.android.messaging.data.conversationlist.store.ConversationListSearchDatabaseStore
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.di.core.MessagingDbDispatcher
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface ConversationListSearchRepository {

    /**
     * Every conversation search can find, archived ones included and blocked senders left out,
     * with the participants whose names and numbers it can be found by.
     */
    fun observeSearchableConversations(): Flow<ImmutableList<SearchableConversation>>
}

internal class ConversationListSearchRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val conversationListRepository: ConversationListRepository,
    private val databaseStore: ConversationListSearchDatabaseStore,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : ConversationListSearchRepository {

    override fun observeSearchableConversations(): Flow<ImmutableList<SearchableConversation>> {
        return combine(
            conversationListRepository.observeSnapshot(mode = ConversationListMode.Inbox),
            conversationListRepository.observeSnapshot(mode = ConversationListMode.Archived),
            observeParticipants(),
        ) { inbox, archived, participantsByConversation ->
            // Only the inbox snapshot reads the blocked senders; they apply to both lists
            (inbox.items + archived.items)
                .filterNot { item ->
                    item.isFromBlockedSender(blockedDestinations = inbox.blockedDestinations)
                }
                .map { item ->
                    SearchableConversation(
                        item = item,
                        participants = participantsByConversation[item.conversationId]
                            ?: persistentListOf(),
                    )
                }
                .toImmutableList()
        }
    }

    private fun observeParticipants():
        Flow<ImmutableMap<ConversationId, ImmutableList<SearchableParticipant>>> {
        return observeUris(
            uris = listOf(
                MessagingContentProvider.CONVERSATIONS_URI,
                MessagingContentProvider.PARTICIPANTS_URI,
            ),
        )
            .conflate()
            .map { databaseStore.readParticipants() }
            .flowOn(messagingDbDispatcher)
    }

    private fun ConversationListItem.isFromBlockedSender(
        blockedDestinations: Set<String>,
    ): Boolean {
        return !participant.isGroup &&
            participant.otherNormalizedDestination in blockedDestinations
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
