@file:JvmName("MmsNotificationDownloadState")

package com.android.messaging.datamodel.action.mms

import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.sms.DatabaseMessages.LocalDatabaseMessage
import com.android.messaging.sms.MmsUtils

internal val mmsNotificationDownloadStatuses = intArrayOf(
    MessageData.BUGLE_STATUS_INCOMING_RETRYING_MANUAL_DOWNLOAD,
    MessageData.BUGLE_STATUS_INCOMING_MANUAL_DOWNLOADING,
    MessageData.BUGLE_STATUS_INCOMING_RETRYING_AUTO_DOWNLOAD,
    MessageData.BUGLE_STATUS_INCOMING_AUTO_DOWNLOADING,
)

internal val mmsNotificationDownloadStatusArgs = Array(mmsNotificationDownloadStatuses.size) {
    mmsNotificationDownloadStatuses[it].toString()
}

internal val mmsNotificationDownloadStatusInOperand = MmsUtils
    .getSqlInOperand(mmsNotificationDownloadStatuses.size)

internal fun shouldProtectMmsNotificationDownload(
    localMessage: LocalDatabaseMessage,
): Boolean {
    return MessageData.getIsMmsNotification(localMessage.protocol) &&
        mmsNotificationDownloadStatuses.contains(localMessage.status) &&
        !isMmsNotificationExpired(
            mmsExpiry = localMessage.mmsExpiry,
            nowMillis = System.currentTimeMillis(),
        )
}

internal fun isMmsNotificationExpired(
    mmsExpiry: Long,
    nowMillis: Long,
): Boolean {
    return mmsExpiry in 1..nowMillis
}
