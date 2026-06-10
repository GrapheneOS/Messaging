package com.android.messaging.domain.conversationlist.usecase

import com.android.messaging.datamodel.action.UpdateConversationArchiveStatusAction
import javax.inject.Inject

internal interface SetConversationArchived {
    operator fun invoke(conversationIds: Set<String>, isArchived: Boolean)
}

internal class SetConversationArchivedImpl @Inject constructor() : SetConversationArchived {

    override operator fun invoke(
        conversationIds: Set<String>,
        isArchived: Boolean,
    ) {
        conversationIds
            .asSequence()
            .map { conversationId ->
                conversationId.trim()
            }
            .filter { conversationId ->
                conversationId.isNotEmpty()
            }
            .forEach { conversationId ->
                when {
                    isArchived -> UpdateConversationArchiveStatusAction.archiveConversation(
                        conversationId,
                    )

                    else -> UpdateConversationArchiveStatusAction.unarchiveConversation(
                        conversationId,
                    )
                }
            }
    }
}
