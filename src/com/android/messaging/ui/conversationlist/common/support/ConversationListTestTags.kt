package com.android.messaging.ui.conversationlist.common.support

import com.android.messaging.data.conversation.model.ConversationId

internal const val CONVERSATION_LIST_TEST_TAG = "conversation_list"

internal const val CONVERSATION_LIST_SEARCH_FIELD_TEST_TAG = "conversation_list_search_field"

internal const val CONVERSATION_LIST_SEARCH_RESULTS_TEST_TAG = "conversation_list_search_results"

internal fun conversationListItemTestTag(conversationId: ConversationId): String {
    return "conversation_list_item_${conversationId.value}"
}
