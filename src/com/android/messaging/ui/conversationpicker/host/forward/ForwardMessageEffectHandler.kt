package com.android.messaging.ui.conversationpicker.host.forward

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.android.messaging.R
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.domain.conversationpicker.model.SendTarget
import com.android.messaging.ui.common.components.attachment.openAttachmentPreview
import com.android.messaging.ui.conversationpicker.ConversationPickerEffectHandler
import com.android.messaging.ui.conversationpicker.model.ConversationPickerEffect as Effect
import com.android.messaging.util.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun rememberForwardMessageEffectHandler(
    onTargetSelected: (ConversationId) -> Unit,
    onSendToSelected: (Set<SendTarget>, ConversationDraft) -> Unit,
): ConversationPickerEffectHandler {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return remember(context, coroutineScope, onTargetSelected, onSendToSelected) {
        ForwardMessageEffectHandler(
            context = context,
            onTargetSelected = onTargetSelected,
            onSendToSelected = onSendToSelected,
            coroutineScope = coroutineScope,
        )
    }
}

internal class ForwardMessageEffectHandler(
    private val context: Context,
    private val onTargetSelected: (ConversationId) -> Unit,
    private val onSendToSelected: (Set<SendTarget>, ConversationDraft) -> Unit,
    private val coroutineScope: CoroutineScope,
) : ConversationPickerEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenConversation -> {
                onTargetSelected(effect.conversationId)
            }

            is Effect.OpenConversationFailed -> {
                UiUtils.showToastAtBottom(R.string.conversation_picker_open_failed)
            }

            is Effect.SendToSelected -> {
                onSendToSelected(effect.targets, effect.draft)
            }

            is Effect.OpenAttachmentPreview -> {
                openPreview(effect.contentUri, effect.contentType)
            }
        }
    }

    private fun openPreview(
        contentUri: String,
        contentType: String,
    ) {
        coroutineScope.launch {
            openAttachmentPreview(
                context = context,
                contentUri = contentUri,
                contentType = contentType,
            )
        }
    }
}
