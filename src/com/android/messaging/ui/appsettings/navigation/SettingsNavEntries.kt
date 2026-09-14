package com.android.messaging.ui.appsettings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.android.messaging.R
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.ui.appsettings.general.AppSettingsViewModel
import com.android.messaging.ui.appsettings.general.model.AppSettingsNavEvent
import com.android.messaging.ui.appsettings.general.rememberAppSettingsEffectHandler
import com.android.messaging.ui.appsettings.general.ui.AppSettingsScreen
import com.android.messaging.ui.appsettings.main.SettingsMainViewModel
import com.android.messaging.ui.appsettings.main.ui.SettingsMainScreen
import com.android.messaging.ui.appsettings.privacy.ui.PrivacySettingsScreen
import com.android.messaging.ui.appsettings.subscription.SubscriptionSettingsViewModel
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsNavEvent
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
import com.android.messaging.ui.appsettings.subscription.rememberSubscriptionSettingsEffectHandler
import com.android.messaging.ui.appsettings.subscription.ui.SubscriptionSettingsScreen
import com.android.messaging.ui.core.CollectEvents
import com.android.messaging.ui.license.navigation.LicenseNavKey
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.Navigator
import com.android.messaging.ui.navigation.SeededViewModelStoreOwner
import com.android.messaging.ui.navigation.paneTitleMetadata
import kotlinx.collections.immutable.ImmutableList

internal fun EntryProviderScope<NavKey>.settingsEntries() {
    entry<SettingsNavKey>(
        metadata = paneTitleMetadata(R.string.settings_activity_title),
        content = settingsRouteContent(),
    )
    entry<AppSettingsNavKey>(
        metadata = paneTitleMetadata(R.string.general_settings_activity_title),
        content = appSettingsRouteContent(),
    )
    entry<PrivacySettingsNavKey>(
        metadata = paneTitleMetadata(R.string.privacy_settings_activity_title),
        content = privacySettingsRouteContent(),
    )
    entry<SubscriptionSettingsNavKey>(
        metadata = paneTitleMetadata(R.string.advanced_settings_activity_title),
        content = subscriptionSettingsRouteContent(),
    )
}

private fun settingsRouteContent(): @Composable (SettingsNavKey) -> Unit {
    return {
        val viewModel = hiltViewModel<SettingsMainViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
            viewModel.refreshState()
        }

        when (uiState.isMultiSim) {
            true -> SettingsMainDestination(
                subscriptions = uiState.subscriptions,
            )

            false -> AppSettingsDestination(
                isTopLevel = true,
                advancedSubscription = uiState.subscriptions.firstOrNull(),
            )

            null -> Unit
        }
    }
}

private fun appSettingsRouteContent(): @Composable (AppSettingsNavKey) -> Unit {
    return {
        AppSettingsDestination(
            isTopLevel = false,
            advancedSubscription = null,
        )
    }
}

@Composable
private fun SettingsMainDestination(subscriptions: ImmutableList<SubscriptionUiState>) {
    val navigator = LocalNavigator.current

    SettingsMainScreen(
        subscriptions = subscriptions,
        onNavigateBack = navigator::back,
        onGeneralSettingsClick = {
            navigator.push(destination = AppSettingsNavKey)
        },
        onSubscriptionClick = { subId, title ->
            navigator.navigateToSubscription(
                subId = subId,
                title = title,
            )
        },
    )
}

@Composable
private fun AppSettingsDestination(
    isTopLevel: Boolean,
    advancedSubscription: SubscriptionUiState?,
) {
    val navigator = LocalNavigator.current
    val viewModel = hiltViewModel<AppSettingsViewModel>()
    val appSettings by viewModel.uiState.collectAsStateWithLifecycle()
    val effectHandler = rememberAppSettingsEffectHandler()

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.refreshState()
    }

    CollectEvents(
        events = viewModel.effects,
        onEvent = effectHandler::handle,
    )

    CollectEvents(events = viewModel.navigationEvents) { event ->
        when (event) {
            AppSettingsNavEvent.OpenLicenses -> {
                navigator.push(destination = LicenseNavKey)
            }
        }
    }

    AppSettingsScreen(
        appSettings = appSettings,
        onAction = viewModel::onAction,
        onNavigateBack = navigator::back,
        onPrivacyClick = {
            navigator.push(destination = PrivacySettingsNavKey)
        },
        isTopLevel = isTopLevel,
        hasAdvancedSettings = advancedSubscription != null,
        onAdvancedClick = {
            advancedSubscription?.let { subscription ->
                navigator.navigateToSubscription(
                    subId = subscription.subId,
                    title = subscription.displayName,
                )
            }
        },
    )
}

private fun privacySettingsRouteContent(): @Composable (PrivacySettingsNavKey) -> Unit {
    return {
        val navigator = LocalNavigator.current
        val viewModel = hiltViewModel<AppSettingsViewModel>()
        val appSettings by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
            viewModel.refreshState()
        }

        PrivacySettingsScreen(
            appSettings = appSettings,
            onAction = viewModel::onAction,
            onNavigateBack = navigator::back,
        )
    }
}

private fun subscriptionSettingsRouteContent(): @Composable (SubscriptionSettingsNavKey) -> Unit {
    return { navKey ->
        val navigator = LocalNavigator.current
        val defaultArgs = remember(navKey) {
            subscriptionSettingsDefaultArgs(navKey = navKey)
        }

        SeededViewModelStoreOwner(defaultArgs = defaultArgs) {
            val viewModel = hiltViewModel<SubscriptionSettingsViewModel>()
            val subscription by viewModel.uiState.collectAsStateWithLifecycle()
            val phoneNumberDialogState by viewModel.phoneNumberDialogState
                .collectAsStateWithLifecycle()
            val effectHandler = rememberSubscriptionSettingsEffectHandler()

            LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
                viewModel.refreshState()
            }

            CollectEvents(
                events = viewModel.effects,
                onEvent = effectHandler::handle,
            )

            CollectEvents(events = viewModel.navigationEvents) { event ->
                when (event) {
                    SubscriptionSettingsNavEvent.Close -> {
                        navigator.back()
                    }
                }
            }

            subscription?.let { subscriptionSettings ->
                SubscriptionSettingsScreen(
                    subscriptionSettings = subscriptionSettings,
                    title = navKey.title,
                    onAction = viewModel::onAction,
                    onNavigateBack = navigator::back,
                    phoneNumberDialogState = phoneNumberDialogState,
                )
            }
        }
    }
}

private fun Navigator.navigateToSubscription(
    subId: SubId,
    title: String,
) {
    push(
        destination = SubscriptionSettingsNavKey(
            subId = subId,
            title = title,
        ),
    )
}
