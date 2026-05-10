package com.android.messaging.ui.appsettings.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.ui.appsettings.common.SettingsClickableItem
import com.android.messaging.ui.appsettings.subscription.model.SubscriptionSettingsUiState
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMainScreen(
    subscriptions: ImmutableList<SubscriptionSettingsUiState>,
    onNavigateBack: (() -> Unit),
    onGeneralSettingsClick: (() -> Unit),
    onSubscriptionClick: ((subId: Int, title: String) -> Unit),
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings_activity_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
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
