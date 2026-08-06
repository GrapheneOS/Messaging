package com.android.messaging.data.conversationpicker.repository

import android.content.ContentResolver
import android.database.ContentObserver
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationpicker.model.TargetConversation
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListData
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.di.core.MessagingDbDispatcher
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface TargetsRepository {
    fun observeTargets(): Flow<ImmutableList<TargetConversation>>
}

internal class TargetsRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : TargetsRepository {

    override fun observeTargets(): Flow<ImmutableList<TargetConversation>> {
        return observeConversations()
            .map { queryTargets() }
            .flowOn(messagingDbDispatcher)
    }

    private fun queryTargets(): ImmutableList<TargetConversation> {
        val cursor = contentResolver.query(
            MessagingContentProvider.CONVERSATIONS_URI,
            ConversationListItemData.PROJECTION,
            ConversationListData.WHERE_NOT_ARCHIVED,
            null,
            ConversationListData.SORT_ORDER,
        ) ?: return persistentListOf()

        return cursor.use {
            buildList(it.count) {
                val item = ConversationListItemData()
                while (it.moveToNext()) {
                    item.bind(it)
                    add(
                        TargetConversation(
                            conversationId = ConversationId(item.conversationId),
                            name = item.name.orEmpty(),
                            icon = item.icon,
                            normalizedDestination = item.otherParticipantNormalizedDestination,
                            isGroup = item.isGroup,
                        ),
                    )
                }
            }
        }.toImmutableList()
    }

    private fun observeConversations(): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }

            contentResolver.registerContentObserver(
                MessagingContentProvider.CONVERSATIONS_URI,
                true,
                observer,
            )

            trySend(Unit)

            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }
}
