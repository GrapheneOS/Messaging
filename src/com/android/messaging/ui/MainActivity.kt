package com.android.messaging.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.host.AppNavGraph
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import com.android.messaging.util.BugleActivityUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    @Inject
    lateinit var shouldShowOnboarding: ShouldShowOnboarding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val startDestination = startDestination()

        setContent {
            AppTheme {
                AppNavGraph(
                    startDestination = startDestination,
                    shouldShowOnboarding = shouldShowOnboarding::invoke,
                    onAppResumed = ::resumeDataModel,
                    onFinish = ::finish,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun resumeDataModel() {
        BugleActivityUtil.onActivityResume(this, this)
    }

    private fun startDestination(): NavKey {
        return when {
            shouldShowOnboarding() -> OnboardingNavKey
            else -> ConversationListNavKey
        }
    }
}
