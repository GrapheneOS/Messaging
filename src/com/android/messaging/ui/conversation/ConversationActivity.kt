package com.android.messaging.ui.conversation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding
import com.android.messaging.ui.MainActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest as LaunchRequest
import com.android.messaging.ui.conversation.navigation.conversationLaunchBackStack
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.host.AppNavGraph
import com.android.messaging.ui.host.toConversationLaunchRequest
import com.android.messaging.util.BugleActivityUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
internal class ConversationActivity : ComponentActivity() {

    @Inject
    lateinit var shouldShowOnboarding: ShouldShowOnboarding

    private val launchRequests = Channel<LaunchRequest?>(Channel.BUFFERED)
    private val launchRequestFlow: Flow<LaunchRequest?> = launchRequests.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.goToConversationList()) {
            redirectToConversationList()
            return
        }

        val request = intent.toConversationLaunchRequest()
        val initialLaunchRequest = when (savedInstanceState) {
            null -> request
            else -> null
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                AppNavGraph(
                    startDestinations = conversationLaunchBackStack(
                        rootDestinations = emptyList(),
                        launchRequest = request,
                    ),
                    conversationRootDestinations = emptyList(),
                    isLaunchedFromBubble = isLaunchedFromBubble,
                    initialLaunchRequest = initialLaunchRequest,
                    launchRequests = launchRequestFlow,
                    shouldShowOnboarding = shouldShowOnboarding::invoke,
                    onAppResumed = ::resumeDataModel,
                    onFinish = ::finishAfterTransition,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        this.intent = intent

        if (intent.goToConversationList()) {
            redirectToConversationList()
            return
        }

        launchRequests.trySend(intent.toConversationLaunchRequest())
    }

    private fun resumeDataModel() {
        BugleActivityUtil.onActivityResume(this, this)
    }

    private fun redirectToConversationList() {
        finish()

        Intent(this, MainActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            .let(::startActivity)
    }

    private fun Intent.goToConversationList(): Boolean {
        return getBooleanExtra(UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST, false)
    }
}
