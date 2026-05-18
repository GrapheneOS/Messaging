package com.android.messaging.domain.conversationsettings.usecase

import android.content.Context
import com.android.messaging.datamodel.action.BugleActionToasts
import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal fun interface SetConversationDestinationBlocked {
    operator fun invoke(
        conversationId: String,
        normalizedDestination: String,
        blocked: Boolean,
    )
}

internal class SetConversationDestinationBlockedImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SetConversationDestinationBlocked {

    override fun invoke(
        conversationId: String,
        normalizedDestination: String,
        blocked: Boolean,
    ) {
        if (normalizedDestination.isEmpty()) return

        UpdateDestinationBlockedAction.updateDestinationBlocked(
            normalizedDestination,
            blocked,
            conversationId,
            BugleActionToasts.makeUpdateDestinationBlockedActionListener(context),
        )
    }
}
