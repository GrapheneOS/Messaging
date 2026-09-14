package com.android.messaging.ui.conversation.entry

import android.content.Intent
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest as LaunchRequest
import com.android.messaging.ui.conversation.navigation.ConversationDraftLauncher
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal interface ConversationLaunchStore {
    val requests: Flow<LaunchRequest>
    fun submit(request: LaunchRequest)
}

@ActivityRetainedScoped
internal class ConversationLaunchStoreImpl @Inject constructor() :
    ConversationLaunchStore,
    ConversationDraftLauncher {

    private val _requests = Channel<LaunchRequest>(Channel.BUFFERED)
    override val requests: Flow<LaunchRequest> = _requests.receiveAsFlow()

    override fun submit(request: LaunchRequest) {
        _requests.trySend(request)
    }

    override fun launch(
        conversationId: ConversationId,
        draft: MessageData?,
    ) {
        submit(
            request = LaunchRequest(
                conversationId = conversationId,
                draftData = draft,
            ),
        )
    }
}

internal fun ConversationLaunchStore.submitIntent(intent: Intent) {
    if (intent.hasConversationLaunchPayload()) {
        submit(intent.toConversationLaunchRequest())
    }
}
