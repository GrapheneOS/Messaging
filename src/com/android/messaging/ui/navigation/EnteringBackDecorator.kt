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
 * Falls back to ordinary back while the top entry is becoming visible.
 *
 * Predictive back can reuse a cached forward enter transition when it interrupts navigation,
 * producing mixed animations. Intercepting it until the entry settles avoids that artifact.
 * Root back remains handled by the system.
 *
 * Only the top entry, keyed by [topContentKey], intercepts. The entry a back swipe reveals is
 * entering too until the swipe's cancel settles, and the platform starts a swipe made meanwhile
 * right away. Intercepting that swipe would pop without seeking, and the handler would go away
 * mid-swipe once the cancel settles; navigationevent then never resets the dispatcher's
 * `transitionState`.
 *
 * Only `PreEnter -> Visible` counts. A cancelled predictive back leaves the entry at
 * `PostExit -> Visible`, which Compose can report as running again later
 * (`SeekableTransitionState.onTotalDurationChanged` seeks a settled transition), so `isRunning`
 * can't tell it from an opening. Intercepting there would disable predictive back for good.
 */
@Composable
internal fun rememberEnteringBackNavEntryDecorator(
    canPop: Boolean,
    topContentKey: Any?,
    onBack: () -> Unit,
): NavEntryDecorator<NavKey> {
    val currentCanPop by rememberUpdatedState(canPop)
    val currentTopContentKey by rememberUpdatedState(topContentKey)
    val currentOnBack by rememberUpdatedState(onBack)

    return remember {
        NavEntryDecorator { entry ->
            val transition = LocalNavAnimatedContentScope.current.transition

            BackHandler(
                enabled = currentCanPop &&
                    entry.contentKey == currentTopContentKey &&
                    transition.currentState == EnterExitState.PreEnter &&
                    transition.targetState == EnterExitState.Visible,
            ) {
                currentOnBack()
            }

            entry.Content()
        }
    }
}
