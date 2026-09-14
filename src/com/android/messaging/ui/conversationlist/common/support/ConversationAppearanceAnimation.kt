package com.android.messaging.ui.conversationlist.common.support

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

internal class AppearanceAnimationToken

@Stable
internal class AppearanceAnimationTracker {

    private var knownConversationIds: Set<ConversationId>? = null
    private val activeTokens = mutableStateMapOf<ConversationId, AppearanceAnimationToken>()
    private val consumedTokens = mutableStateMapOf<ConversationId, AppearanceAnimationToken>()

    fun computeEntering(
        currentConversationIds: Set<ConversationId>,
        isListAtTop: Boolean,
        excludedConversationIds: Set<ConversationId>,
    ): Map<ConversationId, AppearanceAnimationToken> {
        val previousConversationIds = knownConversationIds ?: return emptyMap()

        return when {
            !isListAtTop -> emptyMap()

            else -> {
                currentConversationIds
                    .minus(previousConversationIds)
                    .minus(excludedConversationIds)
                    .associateWith { AppearanceAnimationToken() }
            }
        }
    }

    fun commit(
        currentConversationIds: Set<ConversationId>,
        enteringTokens: Map<ConversationId, AppearanceAnimationToken>,
    ) {
        knownConversationIds = currentConversationIds

        activeTokens.keys
            .filterNot(currentConversationIds::contains)
            .forEach(activeTokens::remove)
        consumedTokens.keys
            .filterNot(currentConversationIds::contains)
            .forEach(consumedTokens::remove)
        activeTokens.putAll(enteringTokens)
    }

    fun tokenFor(
        conversationId: ConversationId,
        enteringTokens: Map<ConversationId, AppearanceAnimationToken>,
    ): AppearanceAnimationToken? {
        val token = enteringTokens[conversationId] ?: activeTokens[conversationId] ?: return null

        return token.takeIf { consumedTokens[conversationId] != it }
    }

    fun onAnimationFinished(
        conversationId: ConversationId,
        token: AppearanceAnimationToken,
    ) {
        if (activeTokens[conversationId] == token) {
            activeTokens.remove(conversationId)
        }
        consumedTokens[conversationId] = token
    }
}

internal class AppearanceAnimationTokens(
    private val tracker: AppearanceAnimationTracker,
    private val enteringTokens: Map<ConversationId, AppearanceAnimationToken>,
) {
    fun tokenFor(conversationId: ConversationId): AppearanceAnimationToken? {
        return tracker.tokenFor(conversationId, enteringTokens)
    }

    fun onAnimationFinished(
        conversationId: ConversationId,
        token: AppearanceAnimationToken,
    ) {
        tracker.onAnimationFinished(conversationId, token)
    }
}

@Composable
internal fun rememberAppearanceAnimationTokens(
    items: ImmutableList<ConversationListItemUiModel>,
    listState: LazyListState,
    excludedConversationIds: ImmutableSet<ConversationId>,
): AppearanceAnimationTokens {
    val tracker = remember { AppearanceAnimationTracker() }
    val currentConversationIds = remember(items) {
        items.mapTo(HashSet(items.size), ConversationListItemUiModel::conversationId)
    }
    val enteringTokens = remember(currentConversationIds, excludedConversationIds) {
        // LazyListState warns these values are observable and frequently changing.
        // Sample them only when the id set changes: new rows animate only if the
        // list is already at the top, and scrolling alone should not invalidate this.
        val isListAtTop = Snapshot.withoutReadObservation {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
        }

        tracker.computeEntering(
            currentConversationIds = currentConversationIds,
            isListAtTop = isListAtTop,
            excludedConversationIds = excludedConversationIds,
        )
    }

    SideEffect {
        tracker.commit(
            currentConversationIds = currentConversationIds,
            enteringTokens = enteringTokens,
        )
    }

    return remember(enteringTokens) {
        AppearanceAnimationTokens(
            tracker = tracker,
            enteringTokens = enteringTokens,
        )
    }
}
