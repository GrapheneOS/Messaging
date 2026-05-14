package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import com.android.messaging.data.conversation.model.notification.LegacyConversationNotificationPrefs
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.PeopleOptionsItemData
import javax.inject.Inject

internal interface ConversationNotificationRepository {
    fun getLegacyNotificationPrefs(conversationId: String): LegacyConversationNotificationPrefs
}

internal class ConversationNotificationRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
) : ConversationNotificationRepository {

    override fun getLegacyNotificationPrefs(
        conversationId: String,
    ): LegacyConversationNotificationPrefs {
        val cursor = contentResolver.query(
            MessagingContentProvider.buildConversationMetadataUri(conversationId),
            PeopleOptionsItemData.PROJECTION,
            null,
            null,
            null,
        )

        return cursor.use {
            if (it == null || !it.moveToFirst()) {
                LegacyConversationNotificationPrefs.Default
            } else {
                LegacyConversationNotificationPrefs(
                    notificationsEnabled = it.getInt(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_ENABLED,
                    ) == 1,
                    ringtoneString = it.getString(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_SOUND_URI,
                    ),
                    vibrationEnabled = it.getInt(
                        PeopleOptionsItemData.INDEX_NOTIFICATION_VIBRATION,
                    ) == 1,
                )
            }
        }
    }
}
