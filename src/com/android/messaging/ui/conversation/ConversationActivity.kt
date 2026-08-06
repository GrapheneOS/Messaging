package com.android.messaging.ui.conversation

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.MainActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.navigation.conversationLaunchBackStack
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.host.AppNavGraph
import com.android.messaging.ui.host.toConversationLaunchRequest
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ConversationActivity : BugleComponentActivity() {

    private var launchGeneration = 0
    private var launchRequest: ConversationEntryLaunchRequest? by mutableStateOf(value = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        launchGeneration = savedInstanceState?.getInt(LAUNCH_GENERATION_STATE_KEY) ?: 0

        if (applyIntent(intent = intent, launchGeneration = launchGeneration)) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                AppNavGraph(
                    startDestinations = conversationLaunchBackStack(
                        rootDestinations = emptyList(),
                        launchRequest = launchRequest,
                    ),
                    conversationRootDestinations = emptyList(),
                    launchRequest = launchRequest,
                    onFinish = ::finishAfterTransition,
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

    private fun applyIntent(
        intent: Intent,
        launchGeneration: Int,
    ): Boolean {
        setIntent(intent)

        val goToConversationList = intent.getBooleanExtra(
            UIIntents.UI_INTENT_EXTRA_GOTO_CONVERSATION_LIST,
            false,
        )

        if (goToConversationList) {
            redirectToConversationList()
            return true
        }

        launchRequest = intent.toConversationLaunchRequest(
            launchGeneration = launchGeneration,
            isLaunchedFromBubble = isLaunchedFromBubble,
        )

        return false
    }

    private fun redirectToConversationList() {
        finish()

        Intent(this, MainActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            .let(::startActivity)
    }

    companion object {
        const val FINISH_RESULT_CODE: Int = 1

        private const val LAUNCH_GENERATION_STATE_KEY = "launch_generation"
    }
}
