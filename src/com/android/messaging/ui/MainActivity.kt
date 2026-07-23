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
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest as LaunchRequest
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    @Inject
    lateinit var shouldShowOnboarding: ShouldShowOnboarding

    private val launchRequests = Channel<LaunchRequest?>(Channel.BUFFERED)
    private val launchRequestFlow: Flow<LaunchRequest?> = launchRequests.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val initialLaunchRequest = when (savedInstanceState) {
            null -> launchRequestForIntent(intent)
            else -> null
        }

        val startDestinations = startDestinations(
            initialLaunchRequest = initialLaunchRequest
        )

        setContent {
            AppTheme {
                AppNavGraph(
                    startDestinations = startDestinations,
                    conversationRootDestinations = listOf(ConversationListNavKey),
                    isLaunchedFromBubble = false,
                    initialLaunchRequest = initialLaunchRequest,
                    launchRequests = launchRequestFlow,
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

            intent.goToConversationList() -> launchRequests.trySend(null)

            intent.hasConversationLaunchPayload() -> {
                launchRequests.trySend(intent.toConversationLaunchRequest())
            }
        }
    }

    private fun launchRequestForIntent(intent: Intent): LaunchRequest? {
        return when {
            shouldShowOnboarding() -> null
            intent.goToConversationList() -> null
            intent.hasConversationLaunchPayload() -> intent.toConversationLaunchRequest()
            else -> null
        }
    }

    private fun resumeDataModel() {
        BugleActivityUtil.onActivityResume(this, this)
    }

    private fun startDestinations(
        initialLaunchRequest: LaunchRequest?,
    ): List<NavKey> {
        if (shouldShowOnboarding()) {
            return listOf(OnboardingNavKey)
        }

        return conversationLaunchBackStack(
            rootDestinations = listOf(ConversationListNavKey),
            launchRequest = initialLaunchRequest,
        )
    }

    private fun Intent.goToConversationList(): Boolean {
        return getBooleanExtra(UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST, false)
    }
}
