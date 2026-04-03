package com.android.messaging.ui.appsettings.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.ui.appsettings.general.ui.AppSettingsScreen
import com.android.messaging.ui.appsettings.screen.model.SettingsNavRoute
import com.android.messaging.ui.appsettings.subscription.ui.SubscriptionSettingsScreen

@Composable
internal fun SettingsScreen(
    onNavigateBack: (() -> Unit),
    modifier: Modifier = Modifier,
    initialRoute: SettingsNavRoute = SettingsNavRoute.Main,
    screenModel: SettingsScreenModel = viewModel<SettingsViewModel>(),
) {
    val context = LocalContext.current
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    var currentRoute by remember {
        mutableStateOf(initialRoute)
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.refreshState()
    }

    val effectHandler = remember(context) { SettingsEffectHandlerImpl(context) }
    LaunchedEffect(screenModel, effectHandler) {
        screenModel.effects.collect(effectHandler::handle)
    }

    // For single-SIM go directly to app settings
    val effectiveRoute = if (uiState.isMultiSim == false && currentRoute is SettingsNavRoute.Main) {
        SettingsNavRoute.AppSettings
    } else {
        currentRoute
    }

    val isRootRoute = effectiveRoute is SettingsNavRoute.Main ||
        (effectiveRoute is SettingsNavRoute.AppSettings && uiState.isMultiSim == false)

    val navigateUp: (() -> Unit) = {
        when {
            isRootRoute -> onNavigateBack()

            effectiveRoute is SettingsNavRoute.AppSettings -> {
                currentRoute = SettingsNavRoute.Main
            }

            effectiveRoute is SettingsNavRoute.SubscriptionSettings -> {
                currentRoute = if (uiState.isMultiSim == true) {
                    SettingsNavRoute.Main
                } else {
                    SettingsNavRoute.AppSettings
                }
            }
        }
    }

    BackHandler(
        enabled = !isRootRoute,
        onBack = navigateUp,
    )

    AnimatedContent(
        targetState = effectiveRoute,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        transitionSpec = {
            val isForward = targetState.depth > initialState.depth
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
                    onSubscriptionClick = { subId, title ->
                        currentRoute = SettingsNavRoute.SubscriptionSettings(subId, title)
                    },
                )
            }

            is SettingsNavRoute.AppSettings -> {
                val isSingleSim = uiState.isMultiSim == false
                AppSettingsScreen(
                    appSettings = uiState.appSettings,
                    screenModel = screenModel,
                    isTopLevel = isSingleSim,
                    onAdvancedClick = uiState.subscriptionSettings
                        .firstOrNull()
                        ?.takeIf { isSingleSim }
                        ?.let { sub ->
                            {
                                currentRoute = SettingsNavRoute.SubscriptionSettings(
                                    sub.subId,
                                    sub.displayName,
                                )
                            }
                        },
                    onNavigateBack = navigateUp,
                )
            }

            is SettingsNavRoute.SubscriptionSettings -> {
                val sub = uiState.subscriptionSettings.find { it.subId == route.subId }
                if (sub != null) {
                    SubscriptionSettingsScreen(
                        subscriptionSettings = sub,
                        title = route.title,
                        screenModel = screenModel,
                        onNavigateBack = navigateUp,
                    )
                }
            }
        }
    }
}
