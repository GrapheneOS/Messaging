package com.android.messaging.ui.appsettings.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsMainScreen(
    subscriptions: List<SubscriptionSettingsUiState>,
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
            item {
                SettingsClickableItem(
                    title = stringResource(R.string.general_settings),
                    onClick = onGeneralSettingsClick,
                )
            }

            items(subscriptions) { subscription ->
                SettingsClickableItem(
                    title = subscription.displayName,
                    summary = subscription.displayDetail,
                    onClick = {
                        onSubscriptionClick(subscription.subId, subscription.displayName)
                    },
                )
            }
        }
    }
}
