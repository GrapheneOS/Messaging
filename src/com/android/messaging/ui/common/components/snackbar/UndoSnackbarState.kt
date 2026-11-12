package com.android.messaging.ui.common.components.snackbar

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class UndoSnackbarBehavior<T : Any>(
    val actionLabel: String,
    val merge: (shown: T, newer: T) -> T?,
    val message: (undoable: T) -> String,
    val onUndo: (undoable: T) -> Unit,
    val onDismiss: (undoable: T) -> Unit,
)

@Composable
internal fun <T : Any> rememberUndoSnackbarState(
    snackbarHostState: SnackbarHostState,
    merge: (shown: T, newer: T) -> T?,
    message: (undoable: T) -> String,
    onUndo: (undoable: T) -> Unit,
    onDismiss: (undoable: T) -> Unit,
): UndoSnackbarState<T> {
    val scope = rememberCoroutineScope()
    val behavior = rememberUpdatedState(
        UndoSnackbarBehavior(
            actionLabel = stringResource(R.string.snack_bar_undo),
            merge = merge,
            message = message,
            onUndo = onUndo,
            onDismiss = onDismiss,
        ),
    )

    return remember(snackbarHostState, scope) {
        UndoSnackbarState(
            snackbarHostState = snackbarHostState,
            scope = scope,
            behavior = behavior,
        )
    }
}

// SnackbarHostState queues snackbars oldest first, so a newer undo would wait behind older ones.
// Instead the newest undo takes over the shown snackbar, merging into it when it is the same kind.
@Stable
internal class UndoSnackbarState<T : Any>(
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
    private val behavior: State<UndoSnackbarBehavior<T>>,
) {
    private var shownSnackbar: ShownUndoSnackbar<T>? = null

    fun show(undoable: T) {
        val shownUndoable = replaceShownSnackbar(newer = undoable)

        shownSnackbar = ShownUndoSnackbar(
            undoable = shownUndoable,
            job = launchSnackbar(undoable = shownUndoable),
        )
    }

    private fun replaceShownSnackbar(newer: T): T {
        val replaced = shownSnackbar ?: return newer

        replaced.job.cancel()

        val merged = behavior.value.merge(replaced.undoable, newer)
        if (merged == null) {
            behavior.value.onDismiss(replaced.undoable)
        }

        return merged ?: newer
    }

    private fun launchSnackbar(undoable: T): Job {
        return scope.launch(Dispatchers.Main.immediate) {
            val isUndone = snackbarHostState.showActionSnackbar(
                message = behavior.value.message(undoable),
                actionLabel = behavior.value.actionLabel,
            )

            shownSnackbar = null

            when {
                isUndone -> behavior.value.onUndo(undoable)
                else -> behavior.value.onDismiss(undoable)
            }
        }
    }
}

private class ShownUndoSnackbar<T : Any>(
    val undoable: T,
    val job: Job,
)
