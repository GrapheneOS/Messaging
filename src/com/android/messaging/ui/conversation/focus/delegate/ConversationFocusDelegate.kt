package com.android.messaging.ui.conversation.focus.delegate

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.di.core.MainDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal interface ConversationFocusDelegate {
    fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<ConversationId?>,
    )

    fun setScreenFocused(
        focused: Boolean,
        cancelNotification: Boolean = true,
    )
}

internal class ConversationFocusDelegateImpl @Inject constructor(
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ConversationFocusDelegate {

    private var conversationIdFlow: StateFlow<ConversationId?>? = null
    private var focusRequest: FocusRequest = FocusRequest.Unfocused
    private var focusedConversation: FocusedConversation? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<ConversationId?>,
    ) {
        if (this.conversationIdFlow != null) {
            return
        }

        this.conversationIdFlow = conversationIdFlow

        scope.launch(mainDispatcher) {
            conversationIdFlow.collect {
                updateFocusedConversation()
            }
        }
    }

    override fun setScreenFocused(
        focused: Boolean,
        cancelNotification: Boolean,
    ) {
        focusRequest = when {
            focused -> FocusRequest.Focused(cancelNotification = cancelNotification)
            else -> FocusRequest.Unfocused
        }
        updateFocusedConversation()
    }

    private fun updateFocusedConversation() {
        val previous = focusedConversation
        val target = targetFocusedConversation()
        focusedConversation = target

        when {
            target == previous -> Unit

            target != null -> {
                DataModel.get().setFocusedConversation(target.conversationId.value)
                BugleNotifications.markMessagesAsRead(
                    target.conversationId.value,
                    target.cancelNotification,
                )
            }

            // Another conversation may have taken the focus since; it keeps it.
            previous != null &&
                DataModel.get().isFocusedConversation(previous.conversationId.value) -> {
                DataModel.get().setFocusedConversation(null)
            }
        }
    }

    private fun targetFocusedConversation(): FocusedConversation? {
        val request = focusRequest
        val conversationId = conversationIdFlow?.value?.takeIf { it.isNotBlank() }

        return when {
            request is FocusRequest.Focused && conversationId != null -> {
                FocusedConversation(
                    conversationId = conversationId,
                    cancelNotification = request.cancelNotification,
                )
            }

            else -> null
        }
    }

    private sealed interface FocusRequest {
        data object Unfocused : FocusRequest
        data class Focused(
            val cancelNotification: Boolean,
        ) : FocusRequest
    }

    private data class FocusedConversation(
        val conversationId: ConversationId,
        val cancelNotification: Boolean,
    )
}
