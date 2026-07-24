package com.android.messaging.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding
import com.android.messaging.ui.conversation.entry.ConversationLaunchStore
import com.android.messaging.ui.conversation.entry.submitIntent
import com.android.messaging.ui.conversation.navigation.conversationRoute
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.host.AppNavGraph
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import com.android.messaging.util.BugleActivityUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    @Inject
    lateinit var shouldShowOnboarding: ShouldShowOnboarding

    @Inject
    lateinit var launchStore: ConversationLaunchStore

    private val launchDestinations = Channel<List<NavKey>>(Channel.BUFFERED)
    private val launchDestinationFlow: Flow<List<NavKey>> = launchDestinations.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val startDestinations = startDestinations(intent = intent)

        if (savedInstanceState == null) {
            submitInitialLaunch(intent = intent)
        }

        setContent {
            AppTheme {
                AppNavGraph(
                    startDestinations = startDestinations,
                    isLaunchedFromBubble = false,
                    launchDestinations = launchDestinationFlow,
                    shouldShowOnboarding = shouldShowOnboarding::invoke,
                    onAppResumed = ::resumeDataModel,
                    onFinish = ::finish,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        this.intent = intent

        when {
            shouldShowOnboarding() -> Unit

            intent.goToConversationList() -> launchDestinations.trySend(rootDestinations())

            else -> {
                val route = conversationRoute(intent) ?: return
                launchStore.submitIntent(intent = intent)
                launchDestinations.trySend(rootDestinations() + route)
            }
        }
    }

    private fun submitInitialLaunch(intent: Intent) {
        val hasLaunchPayload = !shouldShowOnboarding() &&
            !intent.goToConversationList() &&
            conversationRoute(intent) != null

        if (hasLaunchPayload) {
            launchStore.submitIntent(intent = intent)
        }
    }

    private fun resumeDataModel() {
        BugleActivityUtil.onActivityResume(this, this)
    }

    private fun startDestinations(intent: Intent): List<NavKey> {
        if (shouldShowOnboarding()) {
            return listOf(OnboardingNavKey)
        }

        return rootDestinations() + conversationRoute(intent).orEmpty()
    }

    private fun rootDestinations(): List<NavKey> {
        return listOf(ConversationListNavKey)
    }

    private fun Intent.goToConversationList(): Boolean {
        return getBooleanExtra(UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST, false)
    }
}
