package com.android.messaging.ui.appsettings.redesign.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.appsettings.redesign.model.SettingsNavRoute

@Composable
internal fun SettingsScreen(
    onNavigateBack: (() -> Unit),
    modifier: Modifier = Modifier,
    screenModel: SettingsScreenModel = viewModel<SettingsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    var currentRoute by remember {
        mutableStateOf<SettingsNavRoute>(SettingsNavRoute.Main)
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.refreshState()
    }

    // TODO: screen is blinking
    // For single-SIM go directly to app settings
    val effectiveRoute = if (!uiState.isMultiSim && currentRoute is SettingsNavRoute.Main) {
        SettingsNavRoute.AppSettings
    } else {
        currentRoute
    }

    AnimatedContent(
        targetState = effectiveRoute,
        modifier = modifier,
        transitionSpec = {
            val isForward = targetState != SettingsNavRoute.Main
            if (isForward) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "settings_navigation",
    ) { route ->
        when (route) {
            is SettingsNavRoute.Main -> {
                SettingsMainScreen(
                    subscriptions = uiState.subscriptionSettings,
                    onNavigateBack = onNavigateBack,
                    onGeneralSettingsClick = {
                        currentRoute = SettingsNavRoute.AppSettings
                    },
                    onSubscriptionClick = {
                        currentRoute = SettingsNavRoute.SubscriptionSettings
                    },
                )
            }

            is SettingsNavRoute.AppSettings -> {}

            is SettingsNavRoute.SubscriptionSettings -> {}
        }
    }
}
