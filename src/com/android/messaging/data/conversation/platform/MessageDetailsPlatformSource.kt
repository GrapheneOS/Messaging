package com.android.messaging.data.conversation.platform

import android.content.Context
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.sms.MmsUtils
import com.android.messaging.util.DebugUtils
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface MessageDetailsPlatformSource {
    fun activeSubscriptionCount(): Int

    fun cleanseSubject(subject: String?): String?

    fun loadDebug(message: ConversationMessageData): ConversationMessageDetails.Debug?
}

internal class MessageDetailsPlatformSourceImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : MessageDetailsPlatformSource {

    override fun activeSubscriptionCount(): Int {
        return PhoneUtils.getDefault().activeSubscriptionCount
    }

    override fun cleanseSubject(subject: String?): String? {
        return MmsUtils.cleanseMmsSubject(context.resources, subject)
    }

    override fun loadDebug(
        message: ConversationMessageData,
    ): ConversationMessageDetails.Debug? {
        if (!DebugUtils.isDebugEnabled()) {
            return null
        }

        val telephonyUri = message.smsMessageUri
        val conversationId = message.conversationId

        val mms = when {
            message.isMms && telephonyUri != null -> MmsUtils.loadMms(telephonyUri.toUri())
            else -> null
        }

        val conversationTelephonyThreadId = conversationId?.let { id ->
            BugleDatabaseOperations.getThreadId(DataModel.get().database, id)
        }

        val threadRecipients = conversationTelephonyThreadId?.let { threadId ->
            MmsUtils.getRecipientsByThread(threadId)
        }

        return ConversationMessageDetails.Debug(
            messageId = message.messageId,
            telephonyUri = telephonyUri,
            conversationId = conversationId,
            conversationTelephonyThreadId = conversationTelephonyThreadId,
            telephonyThreadId = mms?.mThreadId,
            contentLocationUrl = mms?.mContentLocation,
            threadRecipientIds = conversationTelephonyThreadId?.let { threadId ->
                MmsUtils.getRawRecipientIdsForThread(threadId)
            },
            threadRecipients = threadRecipients?.toString(),
            sender = when {
                mms != null && threadRecipients != null -> {
                    MmsUtils.getMmsSender(threadRecipients, mms.uri)
                }

                else -> null
            },
        )
    }
}
