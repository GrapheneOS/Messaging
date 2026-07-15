package com.android.messaging.ui.conversationpicker.host.forward

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.di.core.ApplicationCoroutineScope
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversationpicker.usecase.BuildConversationDraftFromMessage
import com.android.messaging.domain.conversationpicker.usecase.SendContentToTargets
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationpicker.ConversationPickerScreen
import com.android.messaging.ui.conversationpicker.model.ConversationPickerLabels
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

@AndroidEntryPoint
class ForwardMessageActivity : BugleComponentActivity() {

    @Inject
    @ApplicationCoroutineScope
    internal lateinit var applicationScope: CoroutineScope

    @Inject
    @MainDispatcher
    internal lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    internal lateinit var sendContentToTargets: SendContentToTargets

    @Inject
    internal lateinit var buildConversationDraftFromMessage: BuildConversationDraftFromMessage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = IntentCompat.getParcelableExtra(
            intent,
            UIIntents.UI_INTENT_EXTRA_DRAFT_DATA,
            MessageData::class.java,
        )

        if (message == null) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val draft = remember(message) {
                    buildConversationDraftFromMessage(message)
                }

                val effectHandler = remember(message) {
                    ForwardMessageEffectHandler(
                        applicationScope = applicationScope,
                        mainDispatcher = mainDispatcher,
                        activity = this,
                        message = message,
                        sendContentToTargets = sendContentToTargets,
                    )
                }

                ConversationPickerScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                    allowMultiSelect = true,
                    labels = ConversationPickerLabels.Forward,
                    isInitialDraftLoading = false,
                    initialDraft = draft,
                )
            }
        }
    }
}
