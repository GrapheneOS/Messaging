package com.android.messaging.ui.conversation.focus.delegate

import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.di.core.DefaultDispatcher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal interface ConversationFocusDelegate {
    fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    )

    fun setScreenFocused(
        focused: Boolean,
        cancelNotification: Boolean = true,
    )
}

internal class ConversationFocusDelegateImpl @Inject constructor(
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationFocusDelegate {

    private val focusStateFlow = MutableStateFlow<FocusRequest>(value = FocusRequest.Unfocused)

    private var boundScope: CoroutineScope? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<String?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch(defaultDispatcher) {
            combine(
                focusStateFlow,
                conversationIdFlow,
            ) { focusRequest, conversationId ->
                when (focusRequest) {
                    is FocusRequest.Focused -> {
                        conversationId
                            ?.takeIf { it.isNotBlank() }
                            ?.let { id ->
                                FocusedConversation(
                                    conversationId = id,
                                    cancelNotification = focusRequest.cancelNotification,
                                )
                            }
                    }

                    FocusRequest.Unfocused -> null
                }
            }
                .distinctUntilChanged()
                .collect { focused ->
                    when {
                        focused == null -> {
                            DataModel.get().setFocusedConversation(null)
                        }

                        else -> {
                            DataModel.get().setFocusedConversation(focused.conversationId)

                            BugleNotifications.markMessagesAsRead(
                                focused.conversationId,
                                focused.cancelNotification,
                            )
                        }
                    }
                }
        }
    }

    override fun setScreenFocused(
        focused: Boolean,
        cancelNotification: Boolean,
    ) {
        focusStateFlow.value = when {
            focused -> FocusRequest.Focused(cancelNotification = cancelNotification)
            else -> FocusRequest.Unfocused
        }
    }

    private sealed interface FocusRequest {
        data object Unfocused : FocusRequest
        data class Focused(
            val cancelNotification: Boolean,
        ) : FocusRequest
    }

    private data class FocusedConversation(
        val conversationId: String,
        val cancelNotification: Boolean,
    )
}
