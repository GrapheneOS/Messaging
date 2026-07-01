package com.android.messaging.ui.conversation

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import com.android.messaging.ui.conversation.navigation.ConversationNavGraph
import com.android.messaging.ui.conversationlist.chats.ConversationListActivity
import com.android.messaging.ui.core.AppTheme
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
                ConversationNavGraph(
                    launchRequest = launchRequest,
                    onConversationDetailsClick = ::launchConversationDetails,
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == FINISH_RESULT_CODE) {
            finish()
        }
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

        launchRequest = ConversationEntryLaunchRequest(
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
            messagePosition = intent
                .getIntExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, -1)
                .takeIf { position -> position >= 0 },
            isLaunchedFromBubble = isLaunchedFromBubble,
        )

        intent.removeExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA)
        intent.removeExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION)

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

    private fun launchConversationDetails(conversationId: String) {
        UIIntents.get().launchPeopleAndOptionsActivity(
            this,
            conversationId,
        )
    }

    companion object {
        const val FINISH_RESULT_CODE: Int = 1

        private const val LAUNCH_GENERATION_STATE_KEY = "launch_generation"
    }
}
