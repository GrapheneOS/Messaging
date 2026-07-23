package com.android.messaging.ui.conversation.entry

import android.content.Intent
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@ActivityRetainedScoped
internal class ConversationLaunchStore @Inject constructor() {

    private val _requests = Channel<ConversationEntryLaunchRequest>(Channel.BUFFERED)
    val requests: Flow<ConversationEntryLaunchRequest> = _requests.receiveAsFlow()

    fun submit(request: ConversationEntryLaunchRequest) {
        _requests.trySend(request)
    }
}

internal fun ConversationLaunchStore.submitIntent(intent: Intent) {
    if (intent.hasConversationLaunchPayload()) {
        submit(intent.toConversationLaunchRequest())
    }
}
