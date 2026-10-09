package com.android.messaging.domain.sync.usecase

import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.ParticipantRefresh
import com.android.messaging.util.LogUtil
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

internal interface RepairAfterMessageSync {
    operator fun invoke()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Provider {
        fun repairAfterMessageSync(): RepairAfterMessageSync
    }
}

internal class RepairAfterMessageSyncImpl @Inject constructor(
    private val conversationStateMirror: ConversationStateMirror,
    private val participantDestinationNormalizer: ParticipantDestinationNormalizer,
) : RepairAfterMessageSync {

    override fun invoke() {
        val isRestored = bestEffort(step = "conversation state restore", fallback = false) {
            conversationStateMirror.restoreIfDue()
        }
        // Only after the restore, which looks blocks up by the numbers the older version stored
        val isRenormalized = !conversationStateMirror.isRestorePending() &&
            bestEffort(step = "participant destinations", fallback = false) {
                participantDestinationNormalizer.renormalizeIfPending()
            }

        conversationStateMirror.update()
        if (isRenormalized || isRestored) {
            BugleDatabaseOperations.clearParticipantIdCache()
            bestEffort(step = "participant contacts", fallback = Unit) {
                ParticipantRefresh.refreshParticipants(ParticipantRefresh.REFRESH_MODE_INCREMENTAL)
            }
            MessagingContentProvider.notifyEverythingChanged()
        }
    }

    private inline fun <T> bestEffort(step: String, fallback: T, block: () -> T): T {
        return runCatching(block).getOrElse { exception ->
            LogUtil.w(LogUtil.BUGLE_DATABASE_TAG, "Couldn't repair the $step", exception)
            fallback
        }
    }
}
