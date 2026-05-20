package com.android.messaging.domain.blockedparticipants.usecase

import android.content.Context
import com.android.messaging.datamodel.action.BugleActionToasts
import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface SetDestinationBlocked {
    operator fun invoke(
        normalizedDestination: String,
        blocked: Boolean,
        conversationId: String? = null,
    )
}

internal class SetDestinationBlockedImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SetDestinationBlocked {

    override fun invoke(
        normalizedDestination: String,
        blocked: Boolean,
        conversationId: String?,
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
