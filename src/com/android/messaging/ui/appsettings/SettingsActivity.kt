package com.android.messaging.ui.appsettings

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.screen.SettingsEffectHandlerImpl
import com.android.messaging.ui.appsettings.screen.SettingsScreen
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BugleComponentActivity() {

    @Inject
    lateinit var roleManager: RoleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        val effectHandler = SettingsEffectHandlerImpl(
            activity = this,
            roleManager = roleManager,
        )

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

        setContent {
            AppTheme {
                SettingsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                    intentSubId = subId,
                    intentSubTitle = subTitle,
                    isTopLevelIntent = isTopLevel,
                )
            }
        }
    }
}
