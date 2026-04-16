package com.android.messaging.domain.conversation.usecase

import com.android.messaging.datamodel.action.ActionMonitor
import com.android.messaging.datamodel.action.GetOrCreateConversationAction
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.domain.conversation.usecase.model.ResolveConversationIdResult
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal interface ResolveConversationId {
    suspend operator fun invoke(
        destinations: List<String>,
    ): ResolveConversationIdResult
}

// TODO: Get rid of legacy GetOrCreateConversationAction
internal class ResolveConversationIdImpl @Inject constructor(
    @param:MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) : ResolveConversationId {

    override suspend operator fun invoke(
        destinations: List<String>,
    ): ResolveConversationIdResult {
        val participants = createParticipants(destinations = destinations)

        if (participants.isEmpty()) {
            return ResolveConversationIdResult.EmptyDestinations
        }

        return withContext(context = mainDispatcher) {
            suspendCancellableCoroutine { continuation ->
                var actionMonitor: ActionMonitor? = null

                actionMonitor = GetOrCreateConversationAction.getOrCreateConversation(
                    participants,
                    null,
                    object : GetOrCreateConversationAction.GetOrCreateConversationActionListener {
                        override fun onGetOrCreateConversationSucceeded(
                            monitor: ActionMonitor,
                            data: Any?,
                            conversationId: String,
                        ) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    ResolveConversationIdResult.Resolved(
                                        conversationId = conversationId,
                                    ),
                                )
                            }
                        }

                        override fun onGetOrCreateConversationFailed(
                            monitor: ActionMonitor,
                            data: Any?,
                        ) {
                            if (continuation.isActive) {
                                continuation.resume(ResolveConversationIdResult.NotResolved)
                            }
                        }
                    },
                )

                continuation.invokeOnCancellation {
                    actionMonitor?.unregister()
                }
            }
        }
    }

    private fun createParticipants(
        destinations: List<String>,
    ): ArrayList<ParticipantData> {
        return destinations
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(ParticipantData::getFromRawPhoneBySystemLocale)
            .toCollection(ArrayList(destinations.size))
    }
}
