package com.android.messaging.data.secondaryuser

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepository
import com.android.messaging.data.secondaryuser.model.SecondaryUserNotificationTarget
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.data.ConversationListItemData
import com.android.messaging.datamodel.data.ParticipantData
import javax.inject.Inject

internal interface SecondaryUserNotificationTargetResolver {
    fun resolve(address: String?, subscriptionId: Int): SecondaryUserNotificationTarget
}

internal class SecondaryUserNotificationTargetResolverImpl @Inject constructor(
    private val conversationNotificationRepository: ConversationNotificationRepository,
) : SecondaryUserNotificationTargetResolver {

    override fun resolve(
        address: String?,
        subscriptionId: Int,
    ): SecondaryUserNotificationTarget {
        val database = DataModel.get().database
        val destination = address
            ?.takeIf(String::isNotEmpty)
            ?.let { sender ->
                ParticipantData.getFromRawPhoneBySimLocale(sender, subscriptionId)
                    .normalizedDestination
            }
            ?.takeIf(String::isNotEmpty)

        return when {
            destination == null -> SecondaryUserNotificationTarget.NoConversation

            BugleDatabaseOperations.isBlockedDestination(database, destination) -> {
                SecondaryUserNotificationTarget.Silenced
            }

            else -> {
                resolveConversation(database = database, destination = destination)
            }
        }
    }

    private fun resolveConversation(
        database: DatabaseWrapper,
        destination: String,
    ): SecondaryUserNotificationTarget {
        val conversation = newestConversationIdFor(database = database, destination = destination)
            ?.let { conversationId ->
                ConversationListItemData.getExistingConversation(database, conversationId)
            }

        return when {
            conversation == null -> SecondaryUserNotificationTarget.NoConversation

            conversationNotificationRepository.isSnoozed(
                conversationId = ConversationId(conversation.conversationId),
            ) -> SecondaryUserNotificationTarget.Silenced

            else -> {
                SecondaryUserNotificationTarget.Conversation(
                    conversationId = conversation.conversationId,
                    conversationTitle = conversation
                        .name
                        ?.takeIf(String::isNotEmpty)
                        ?: destination,
                )
            }
        }
    }

    private fun newestConversationIdFor(database: DatabaseWrapper, destination: String): String? {
        return database.query(
            DatabaseHelper.CONVERSATIONS_TABLE,
            arrayOf(ConversationColumns._ID),
            "${ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION}=?",
            arrayOf(destination),
            null,
            null,
            "${ConversationColumns.SORT_TIMESTAMP} DESC",
            "1",
        ).use { cursor ->
            when {
                cursor.moveToFirst() -> cursor.getString(0)
                else -> null
            }
        }
    }
}
