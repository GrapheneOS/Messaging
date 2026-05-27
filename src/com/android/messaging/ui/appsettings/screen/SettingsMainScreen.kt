package com.android.messaging.ui.appsettings.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.appsettings.common.SettingsClickableItem
import com.android.messaging.ui.appsettings.common.SettingsTopAppBar
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionUiState
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMainScreen(
    subscriptions: ImmutableList<SubscriptionUiState>,
    onNavigateBack: () -> Unit,
    onGeneralSettingsClick: () -> Unit,
    onSubscriptionClick: (subId: Int, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsTopAppBar(
                title = stringResource(R.string.settings_activity_title),
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item(key = "general_settings") {
                SettingsClickableItem(
                    title = stringResource(R.string.general_settings),
                    onClick = onGeneralSettingsClick,
                )
            }

            if (subscriptions.isNotEmpty()) {
                item(key = "subscriptions_divider") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            itemsIndexed(subscriptions, key = { _, item -> item.subId }) { index, subscription ->
                SettingsClickableItem(
                    title = subscription.displayName,
                    summary = subscription.displayDetail,
                    onClick = {
                        onSubscriptionClick(subscription.subId, subscription.displayName)
                    },
                )

                if (index < subscriptions.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsMainScreenSingleSimPreview() {
    MessagingPreviewTheme {
        SettingsMainScreen(
            subscriptions = persistentListOf(),
            onNavigateBack = {},
            onGeneralSettingsClick = {},
            onSubscriptionClick = { _, _ -> },
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsMainScreenMultiSimPreview() {
    MessagingPreviewTheme {
        SettingsMainScreen(
            subscriptions = persistentListOf(
                SubscriptionUiState(
                    subId = 1,
                    displayName = "SIM 1",
                    displayDetail = "+31 6 1234 5678",
                ),
                SubscriptionUiState(
                    subId = 2,
                    displayName = "Travel SIM",
                    displayDetail = "+372 5555 0101",
                ),
            ),
            onNavigateBack = {},
            onGeneralSettingsClick = {},
            onSubscriptionClick = { _, _ -> },
        )
    }
}
