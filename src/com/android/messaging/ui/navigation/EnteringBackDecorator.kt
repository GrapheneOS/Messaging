package com.android.messaging.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterExitState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/**
 * Falls back to ordinary back while an entry is becoming visible.
 *
 * Predictive back can reuse a cached forward enter transition when it interrupts navigation,
 * producing mixed animations. Intercepting it until the entry settles avoids that artifact.
 * Root back remains handled by the system.
 */
@Composable
internal fun rememberEnteringBackNavEntryDecorator(
    canPop: Boolean,
    onBack: () -> Unit,
): NavEntryDecorator<NavKey> {
    val currentCanPop by rememberUpdatedState(canPop)
    val currentOnBack by rememberUpdatedState(onBack)

    return remember {
        NavEntryDecorator { entry ->
            val transition = LocalNavAnimatedContentScope.current.transition

            BackHandler(
                enabled = currentCanPop &&
                    transition.targetState == EnterExitState.Visible &&
                    transition.currentState != EnterExitState.Visible,
            ) {
                currentOnBack()
            }

            entry.Content()
        }
    }
}
