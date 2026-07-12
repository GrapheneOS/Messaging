package com.android.messaging.data.secondaryuser

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.messaging.R
import com.android.messaging.data.secondaryuser.model.SecondaryUserMessageInfo
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.LogUtil
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.util.PendingIntentConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface SecondaryUserNotifier {
    fun notifyIncomingMessage(info: SecondaryUserMessageInfo?)
    fun cancel()
}

internal class SecondaryUserNotifierImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : SecondaryUserNotifier {

    override fun notifyIncomingMessage(info: SecondaryUserMessageInfo?) {
        val fallback = context.getString(R.string.secondary_user_new_message_title)
        val title = info?.sender ?: fallback
        val text = info?.body ?: fallback
        val style = NotificationCompat.BigTextStyle().bigText(text)
        val pendingIntent = UIIntents.get()
            .getPendingIntentForSecondaryUserNewMessageNotification(context)

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannelUtil.INCOMING_MESSAGES,
        )
            .setContentTitle(title)
            .setContentText(text)
            .setTicker(context.getString(R.string.secondary_user_new_message_ticker))
            .setSmallIcon(R.drawable.ic_sms_light)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pendingIntent)
            .setStyle(style)
            .build()

        try {
            notificationManager().notify(
                notificationTag(),
                PendingIntentConstants.SMS_SECONDARY_USER_NOTIFICATION_ID,
                notification,
            )
        } catch (exception: SecurityException) {
            LogUtil.e(
                LogUtil.BUGLE_TAG,
                "Missing permission to post secondary user notification",
                exception,
            )
        }
    }

    override fun cancel() {
        notificationManager().cancel(
            notificationTag(),
            PendingIntentConstants.SMS_SECONDARY_USER_NOTIFICATION_ID,
        )
    }

    private fun notificationManager(): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

    private fun notificationTag(): String {
        return context.packageName + ":secondaryuser"
    }
}
