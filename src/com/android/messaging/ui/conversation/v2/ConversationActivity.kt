package com.android.messaging.ui.conversation.v2

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.v2.screen.ConversationScreen
import com.android.messaging.ui.conversation.v2.screen.model.ConversationLaunchRequest
import com.android.messaging.ui.conversationlist.ConversationListActivity
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ConversationActivity : ComponentActivity() {

    private var launchGeneration = 0
    private var launchRequest: ConversationLaunchRequest? by mutableStateOf(value = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launchGeneration = savedInstanceState?.getInt(LAUNCH_GENERATION_STATE_KEY) ?: 0

        if (applyIntent(intent = intent, launchGeneration = launchGeneration)) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                ConversationScreen(
                    launchRequest = launchRequest,
                    onNavigateBack = ::finishAfterTransition,
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

        launchRequest = ConversationLaunchRequest(
            launchGeneration = launchGeneration,
            conversationId = intent
                .getStringExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID),
            draftData = intent.getParcelableExtra(
                UIIntents.UI_INTENT_EXTRA_DRAFT_DATA,
                MessageData::class.java,
            ),
            startupAttachmentUri = intent
                .getStringExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_URI)
                ?.takeUnless(TextUtils::isEmpty),
            startupAttachmentType = intent
                .getStringExtra(UIIntents.UI_INTENT_EXTRA_ATTACHMENT_TYPE)
                ?.takeUnless(TextUtils::isEmpty),
        )

        intent.removeExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA)

        return false
    }

    private fun redirectToConversationList() {
        finish()

        Intent(this, ConversationListActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            .let(::startActivity)
    }

    private companion object {
        private const val LAUNCH_GENERATION_STATE_KEY = "launch_generation"
    }
}
