package com.android.messaging.ui.conversationlist.common

internal const val CONVERSATION_LIST_TEST_TAG = "conversation_list"

internal fun conversationListItemTestTag(conversationId: String): String {
    return "conversation_list_item_$conversationId"
}
