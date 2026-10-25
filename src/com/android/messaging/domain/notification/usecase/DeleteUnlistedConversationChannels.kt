package com.android.messaging.domain.notification.usecase

import com.android.messaging.data.extension.runInTransaction
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.di.core.MessagingDbDispatcher
import com.android.messaging.util.LogUtil
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.util.core.extension.unitFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

internal interface DeleteUnlistedConversationChannels {
    operator fun invoke(): Flow<Unit>
}

internal class DeleteUnlistedConversationChannelsImpl @Inject constructor(
    @param:MessagingDbDispatcher
    private val messagingDbDispatcher: CoroutineDispatcher,
) : DeleteUnlistedConversationChannels {

    override operator fun invoke(): Flow<Unit> {
        return unitFlow {
            // An incoming message must not list a conversation between the check and the delete,
            // since deleting the channel would cancel the message's notification
            runInTransaction {
                val channelConversationIds = queryChannelConversationIds()
                val unlistedConversationIds = channelConversationIds - queryListedConversationIds()

                unlistedConversationIds.forEach(NotificationChannelUtil::deleteChannel)
            }
        }.catch { exception ->
            LogUtil.w(TAG, "Could not delete unlisted conversation channels", exception)
        }.flowOn(messagingDbDispatcher)
    }

    private fun queryChannelConversationIds(): Set<String> {
        return NotificationChannelUtil
            .getNotificationManager()
            .notificationChannels
            .filter { it.parentChannelId == NotificationChannelUtil.INCOMING_MESSAGES }
            .mapNotNullTo(mutableSetOf()) { it.conversationId }
    }

    private fun queryListedConversationIds(): Set<String> {
        return DataModel.get().database.query(
            DatabaseHelper.CONVERSATIONS_TABLE,
            LISTED_PROJECTION,
            LISTED_SELECTION,
            null,
            null,
            null,
            null,
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }
    }

    private companion object {
        private const val TAG = "UnlistedConversationChannels"

        private val LISTED_PROJECTION = arrayOf(ConversationColumns._ID)

        // The conversation list hides conversations with no messages or draft this way
        private const val LISTED_SELECTION = "${ConversationColumns.SORT_TIMESTAMP} > 0"
    }
}
