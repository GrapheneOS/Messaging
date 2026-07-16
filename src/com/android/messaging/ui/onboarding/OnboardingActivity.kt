package com.android.messaging.ui.onboarding

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.onboarding.screen.OnboardingEffectHandlerImpl
import com.android.messaging.ui.onboarding.screen.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {

    @Inject
    lateinit var roleManager: RoleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val effectHandler = OnboardingEffectHandlerImpl(
            activity = this,
            roleManager = roleManager,
        )

        setContent {
            AppTheme {
                OnboardingScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
