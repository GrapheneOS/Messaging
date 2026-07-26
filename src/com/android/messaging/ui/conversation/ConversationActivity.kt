package com.android.messaging.ui.conversation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation3.runtime.NavKey
import com.android.messaging.domain.onboarding.usecase.ShouldShowOnboarding
import com.android.messaging.ui.MainActivity
import com.android.messaging.ui.conversation.entry.ConversationLaunchStore
import com.android.messaging.ui.conversation.entry.submitIntent
import com.android.messaging.ui.conversation.navigation.NewChatNavKey
import com.android.messaging.ui.conversation.navigation.conversationRoute
import com.android.messaging.ui.conversationlist.navigation.goToConversationList
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.host.AppNavGraph
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

    @Inject
    lateinit var launchStore: ConversationLaunchStore

    private val launchDestinations = Channel<List<NavKey>>(Channel.BUFFERED)
    private val launchDestinationFlow: Flow<List<NavKey>> = launchDestinations.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.goToConversationList()) {
            redirectToConversationList()
            return
        }

        val startDestinations = conversationRoute(intent) ?: listOf(NewChatNavKey)

        if (savedInstanceState == null) {
            launchStore.submitIntent(intent = intent)
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                AppNavGraph(
                    startDestinations = startDestinations,
                    isLaunchedFromBubble = isLaunchedFromBubble,
                    launchDestinations = launchDestinationFlow,
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

        val route = conversationRoute(intent) ?: return
        launchStore.submitIntent(intent = intent)
        launchDestinations.trySend(route)
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
}
