package com.android.messaging.ui.appsettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.screen.SettingsScreen
import com.android.messaging.ui.appsettings.screen.model.SettingsNavRoute
import com.android.messaging.ui.appsettings.subscription.mapper.SubscriptionSettingsUiStateMapper
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    internal lateinit var subscriptionMapper: SubscriptionSettingsUiStateMapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val initialRoute = resolveInitialRoute()

        setContent {
            AppTheme {
                SettingsScreen(
                    onNavigateBack = ::finish,
                    initialRoute = initialRoute,
                )
            }
        }
    }

    private fun resolveInitialRoute(): SettingsNavRoute {
        val subId = intent.getIntExtra(
            UIIntents.UI_INTENT_EXTRA_SUB_ID,
            ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        val subTitle = intent.getStringExtra(
            UIIntents.UI_INTENT_EXTRA_PER_SUBSCRIPTION_SETTING_TITLE,
        )
        val isTopLevel = intent.getBooleanExtra(
            UIIntents.UI_INTENT_EXTRA_TOP_LEVEL_SETTINGS,
            false,
        )

        return when {
            subTitle != null -> SettingsNavRoute.SubscriptionSettings(subId, subTitle)
            isTopLevel || !subscriptionMapper.isMultiSim() -> SettingsNavRoute.AppSettings
            else -> SettingsNavRoute.Main
        }
    }
}
