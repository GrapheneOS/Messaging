package com.android.messaging.datamodel.data

import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMessageDataTest {

    @Test
    fun notificationQuerySqlIncludesBlockedLogic() {
        val querySql = ConversationMessageData.getNotificationQuerySql().lowercase()

        assertTrue(
            "SQL must use ifnull for blocked status",
            querySql.contains("ifnull(")
        )
        assertTrue(
            "SQL must evaluate null blocked status to 0",
            querySql.contains(", 0) = 0")
        )
    }
}
