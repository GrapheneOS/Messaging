package com.android.messaging.data.subscription.repository

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.util.BuglePrefs
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal interface ConversationSimSelectionRepository {
    fun observe(conversationId: ConversationId): Flow<ParticipantId?>
    fun getSelectedSelfId(conversationId: ConversationId): ParticipantId?
    fun setSelectedSelfId(conversationId: ConversationId, selfId: ParticipantId)
}

internal class ConversationSimSelectionRepositoryImpl @Inject constructor() :
    ConversationSimSelectionRepository {

    private val changes = MutableSharedFlow<ConversationId>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun observe(conversationId: ConversationId): Flow<ParticipantId?> {
        return changes
            .filter { it == conversationId }
            .map { getSelectedSelfId(conversationId) }
            .onStart { emit(getSelectedSelfId(conversationId)) }
            .distinctUntilChanged()
    }

    override fun getSelectedSelfId(conversationId: ConversationId): ParticipantId? {
        if (conversationId.isBlank()) return null

        val prefs = BuglePrefs.getApplicationPrefs()
        return ParticipantId
            .fromOrNull(prefs.getString(prefKey(conversationId), null))
            ?.takeIf { it.isNotBlank() }
    }

    override fun setSelectedSelfId(
        conversationId: ConversationId,
        selfId: ParticipantId,
    ) {
        if (conversationId.isBlank() || selfId.isBlank()) return

        val prefs = BuglePrefs.getApplicationPrefs()
        prefs.putString(prefKey(conversationId), selfId.value)
        changes.tryEmit(conversationId)
    }

    private fun prefKey(conversationId: ConversationId): String {
        return "$PREF_KEY_PREFIX${conversationId.value}"
    }

    private companion object {
        const val PREF_KEY_PREFIX = "conversation_sim_selection_"
    }
}
