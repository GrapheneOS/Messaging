package com.android.messaging.domain.conversationlist.usecase

import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction
import com.android.messaging.di.core.MainDispatcher
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal interface SetConversationBlocked {
    suspend operator fun invoke(
        destination: String,
        conversationId: String,
        isBlocked: Boolean,
    ): Boolean
}

internal class SetConversationBlockedImpl @Inject constructor(
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : SetConversationBlocked {

    override suspend operator fun invoke(
        destination: String,
        conversationId: String,
        isBlocked: Boolean,
    ): Boolean {
        val resolvedDestination = destination.takeIf(String::isNotBlank) ?: return false

        return withContext(mainDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = UpdateDestinationBlockedAction
                    .UpdateDestinationBlockedActionListener { _, success, _, _ ->
                        if (continuation.isActive) {
                            continuation.resume(success)
                        }
                    }

                val actionMonitor = UpdateDestinationBlockedAction.updateDestinationBlocked(
                    resolvedDestination,
                    isBlocked,
                    conversationId.takeIf(String::isNotBlank),
                    listener,
                )

                continuation.invokeOnCancellation {
                    actionMonitor?.unregister()
                }
            }
        }
    }
}
