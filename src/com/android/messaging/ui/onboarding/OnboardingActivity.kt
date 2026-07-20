package com.android.messaging.ui.onboarding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.onboarding.screen.OnboardingScreen
import com.android.messaging.ui.onboarding.screen.rememberOnboardingEffectHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AppTheme {
                OnboardingScreen(
                    effectHandler = rememberOnboardingEffectHandler(),
                    onNavigateBack = ::finish,
                    onOnboardingComplete = ::redirectToConversationList,
                )
            }
        }
    }

    private fun redirectToConversationList() {
        UIIntents.get().launchConversationListActivity(this)
        finish()
    }
}
