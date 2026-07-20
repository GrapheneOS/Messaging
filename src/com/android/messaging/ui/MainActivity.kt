package com.android.messaging.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.navigation.conversationLaunchBackStack
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.host.AppNavGraph
import com.android.messaging.ui.host.hasConversationLaunchPayload
import com.android.messaging.ui.host.toConversationLaunchRequest
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import com.android.messaging.util.BugleActivityUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    @Inject
    lateinit var shouldShowOnboarding: ShouldShowOnboarding

    private var launchGeneration = 0
    private var launchRequest: ConversationEntryLaunchRequest? by mutableStateOf(value = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        launchGeneration = savedInstanceState?.getInt(LAUNCH_GENERATION_STATE_KEY) ?: 0
        applyIntent(
            intent = intent,
            launchGeneration = launchGeneration,
        )

        val startDestinations = startDestinations()

        setContent {
            AppTheme {
                AppNavGraph(
                    startDestinations = startDestinations,
                    conversationRootDestinations = listOf(ConversationListNavKey),
                    launchRequest = launchRequest,
                    onFinish = ::finish,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        launchGeneration += 1
        applyIntent(intent = intent, launchGeneration = launchGeneration)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(LAUNCH_GENERATION_STATE_KEY, launchGeneration)
    }

    override fun onResume() {
        super.onResume()

        if (shouldShowOnboarding()) {
            return
        }

        resumeDataModel()
    }

    private fun applyIntent(
        intent: Intent,
        launchGeneration: Int,
    ) {
        setIntent(intent)

        if (shouldShowOnboarding()) {
            return
        }

        val goToConversationList = intent.getBooleanExtra(
            UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST,
            false,
        )

        launchRequest = when {
            goToConversationList -> null

            intent.hasConversationLaunchPayload() -> intent.toConversationLaunchRequest(
                launchGeneration = launchGeneration,
                isLaunchedFromBubble = false,
            )

            else -> launchRequest
        }
    }

    private fun resumeDataModel() {
        BugleActivityUtil.onActivityResume(this, this)
    }

    private fun startDestinations(): List<NavKey> {
        if (shouldShowOnboarding()) {
            return listOf(OnboardingNavKey)
        }

        return conversationLaunchBackStack(
            rootDestinations = listOf(ConversationListNavKey),
            launchRequest = launchRequest,
        )
    }

    private companion object {
        private const val LAUNCH_GENERATION_STATE_KEY = "launch_generation"
    }
}
