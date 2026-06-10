package com.android.messaging.data.conversationlist.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import com.android.messaging.data.conversationlist.model.ConversationListDraft
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListLatestMessage
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.data.conversationlist.model.ConversationListNotification
import com.android.messaging.data.conversationlist.model.ConversationListParticipant
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.store.ConversationListStatusStore
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListData
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.db.ext.getStringOrNull
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal interface ConversationListRepository {
    fun observeInboxSnapshot(): Flow<ConversationListSnapshot>
    fun setNewestConversationVisible(isVisible: Boolean)
}

internal class ConversationListRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val statusStore: ConversationListStatusStore,
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : ConversationListRepository {

    override fun observeInboxSnapshot(): Flow<ConversationListSnapshot> {
        val itemsFlow = observeUri(MessagingContentProvider.CONVERSATIONS_URI)
            .conflate()
            .map { queryInboxConversations() }

        val blockedDestinationsFlow = observeUri(MessagingContentProvider.PARTICIPANTS_URI)
            .conflate()
            .map { queryBlockedParticipantDestinations() }

        return combine(
            itemsFlow,
            blockedDestinationsFlow,
        ) { items, blockedDestinations ->
            ConversationListSnapshot(
                items = items,
                blockedDestinations = blockedDestinations,
                hasFirstSyncCompleted = statusStore.hasFirstSyncCompleted(),
            )
        }.flowOn(messagingDbDispatcher)
    }

    override fun setNewestConversationVisible(isVisible: Boolean) {
        statusStore.setNewestConversationVisible(isVisible)
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

    private fun queryInboxConversations(): ImmutableList<ConversationListItem> {
        val cursor = contentResolver.query(
            MessagingContentProvider.CONVERSATIONS_URI,
            ConversationListItemData.PROJECTION,
            ConversationListData.WHERE_NOT_ARCHIVED,
            null,
            ConversationListData.SORT_ORDER,
        ) ?: return persistentListOf()

        return cursor.use { conversationCursor ->
            buildList(capacity = conversationCursor.count) {
                val item = ConversationListItemData()

                while (conversationCursor.moveToNext()) {
                    item.bind(conversationCursor)
                    item.toConversationListItem()?.let(::add)
                }
            }
        }.toImmutableList()
    }

    private fun queryBlockedParticipantDestinations(): ImmutableSet<String> {
        val cursor = contentResolver.query(
            MessagingContentProvider.PARTICIPANTS_URI,
            BLOCKED_PARTICIPANTS_PROJECTION,
            "${ParticipantColumns.BLOCKED}=1",
            null,
            null,
        ) ?: return persistentSetOf()

        return cursor.use { blockedParticipantsCursor ->
            buildSet(capacity = blockedParticipantsCursor.count) {
                while (blockedParticipantsCursor.moveToNext()) {
                    blockedParticipantsCursor
                        .getStringOrNull(ParticipantColumns.NORMALIZED_DESTINATION)
                        ?.takeIf(String::isNotBlank)
                        ?.let(::add)
                }
            }
        }.toImmutableSet()
    }

    private fun ConversationListItemData.toConversationListItem(): ConversationListItem? {
        val resolvedConversationId = conversationId
            ?.takeIf(String::isNotBlank)
            ?: return null

        return ConversationListItem(
            conversationId = resolvedConversationId,
            title = name,
            icon = icon,
            subject = subject,
            isArchived = isArchived,
            participant = toParticipant(),
            latestMessage = toLatestMessage(),
            draft = toDraft(),
            notification = ConversationListNotification(
                isEnabled = notificationEnabled,
            ),
        )
    }

    private fun ConversationListItemData.toParticipant(): ConversationListParticipant {
        return ConversationListParticipant(
            contactId = participantContactId,
            lookupKey = participantLookupKey,
            otherNormalizedDestination = otherParticipantNormalizedDestination,
            selfId = selfId,
            count = participantCount,
            isGroup = isGroup,
            includeEmailAddress = includeEmailAddress,
            isEnterprise = isEnterprise,
        )
    }

    private fun ConversationListItemData.toLatestMessage(): ConversationListLatestMessage {
        return ConversationListLatestMessage(
            isRead = isRead,
            timestamp = timestamp,
            snippetText = snippetText,
            previewUri = previewUri?.toString(),
            previewContentType = previewContentType,
            status = mapMessageStatus(
                status = messageStatus,
                rawTelephonyStatus = messageRawTelephonyStatus,
            ),
            isIncoming = MessageData.getIsIncoming(messageStatus),
            senderName = snippetSenderName,
        )
    }

    private fun mapMessageStatus(
        status: Int,
        rawTelephonyStatus: Int,
    ): ConversationListMessageStatus {
        return when (status) {
            MessageData.BUGLE_STATUS_OUTGOING_DRAFT -> {
                ConversationListMessageStatus.Draft
            }

            MessageData.BUGLE_STATUS_UNKNOWN -> {
                ConversationListMessageStatus.Unknown
            }

            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND,
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY,
            MessageData.BUGLE_STATUS_OUTGOING_SENDING,
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING,
            -> {
                ConversationListMessageStatus.Sending
            }

            MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER,
            MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED,
            MessageData.BUGLE_STATUS_INCOMING_EXPIRED_OR_NOT_AVAILABLE,
            -> {
                ConversationListMessageStatus.Failed(rawTelephonyStatus)
            }

            else -> ConversationListMessageStatus.Normal
        }
    }

    private fun ConversationListItemData.toDraft(): ConversationListDraft {
        return ConversationListDraft(
            isVisible = showDraft,
            snippetText = draftSnippetText,
            previewUri = draftPreviewUri?.toString(),
            previewContentType = draftPreviewContentType,
            subject = draftSubject,
        )
    }

    private companion object {
        private val BLOCKED_PARTICIPANTS_PROJECTION = arrayOf(
            ParticipantColumns._ID,
            ParticipantColumns.NORMALIZED_DESTINATION,
        )
    }
}
