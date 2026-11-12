package com.android.messaging.ui.conversationlist.chats

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.messaging.R
import com.android.messaging.ui.common.components.snackbar.UndoSnackbarState
import com.android.messaging.ui.common.components.snackbar.rememberUndoSnackbarState
import com.android.messaging.ui.conversationlist.chats.model.ConversationListAction as Action
import com.android.messaging.ui.conversationlist.chats.model.ConversationListEffect as Effect
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun rememberConversationListUndoSnackbarState(
    snackbarHostState: SnackbarHostState,
    onAction: (Action) -> Unit,
): UndoSnackbarState<Effect.UndoSnackbarEffect> {
    val context = LocalContext.current

    return rememberUndoSnackbarState(
        snackbarHostState = snackbarHostState,
        merge = ::mergeUndoSnackbarEffects,
        message = { effect -> undoSnackbarMessage(context = context, effect = effect) },
        onUndo = { effect -> onAction(undoAction(effect)) },
        onDismiss = { effect -> undoDismissedAction(effect)?.let(onAction) },
    )
}

private fun mergeUndoSnackbarEffects(
    shown: Effect.UndoSnackbarEffect,
    newer: Effect.UndoSnackbarEffect,
): Effect.UndoSnackbarEffect? {
    return when {
        shown is Effect.ArchiveStatusChanged &&
            newer is Effect.ArchiveStatusChanged &&
            shown.isArchived == newer.isArchived -> {
            Effect.ArchiveStatusChanged(
                conversationIds = (shown.conversationIds + newer.conversationIds)
                    .toImmutableList(),
                pinnedConversationIds = (shown.pinnedConversationIds + newer.pinnedConversationIds)
                    .toImmutableList(),
                isArchived = shown.isArchived,
            )
        }

        else -> null
    }
}

private fun undoSnackbarMessage(
    context: Context,
    effect: Effect.UndoSnackbarEffect,
): String {
    return when (effect) {
        is Effect.ArchiveStatusChanged -> {
            val messageResId = when {
                effect.isArchived -> R.string.archived_toast_message
                else -> R.string.unarchived_toast_message
            }

            context.getString(messageResId, effect.conversationIds.size)
        }

        is Effect.ConversationBlocked -> context.getString(R.string.update_destination_blocked)
    }
}

private fun undoAction(effect: Effect.UndoSnackbarEffect): Action {
    return when (effect) {
        is Effect.ArchiveStatusChanged -> {
            Action.ArchiveUndoClicked(
                conversationIds = effect.conversationIds,
                pinnedConversationIds = effect.pinnedConversationIds,
                isArchived = effect.isArchived,
            )
        }

        is Effect.ConversationBlocked -> {
            Action.BlockUndoClicked(
                conversationId = effect.conversationId,
                destination = effect.destination,
                wasPinned = effect.wasPinned,
            )
        }
    }
}

private fun undoDismissedAction(effect: Effect.UndoSnackbarEffect): Action? {
    return when (effect) {
        is Effect.ArchiveStatusChanged -> {
            Action.ArchiveSnackbarDismissed(conversationIds = effect.conversationIds)
        }

        is Effect.ConversationBlocked -> null
    }
}
