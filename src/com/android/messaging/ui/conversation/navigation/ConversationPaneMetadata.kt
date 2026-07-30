@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.android.messaging.ui.conversation.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import com.android.messaging.ui.common.components.ListDetailPaneSide
import com.android.messaging.ui.conversation.screen.ConversationDetailPlaceholder
import com.android.messaging.ui.navigation.listDetailPaneMetadata

private const val CONVERSATION_LIST_DETAIL_SCENE_KEY = "conversation-list-detail"

internal fun conversationListPaneMetadata(): Map<String, Any> {
    return ListDetailSceneStrategy.listPane(
        sceneKey = CONVERSATION_LIST_DETAIL_SCENE_KEY,
        detailPlaceholder = { ConversationDetailPlaceholder() },
    ) + listDetailPaneMetadata(side = ListDetailPaneSide.Start)
}

internal fun conversationDetailPaneMetadata(): Map<String, Any> {
    return ListDetailSceneStrategy.detailPane(
        sceneKey = CONVERSATION_LIST_DETAIL_SCENE_KEY,
    ) + listDetailPaneMetadata(side = ListDetailPaneSide.End)
}
