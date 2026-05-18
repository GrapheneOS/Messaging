package com.android.messaging.data.subscription.repository

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
    fun observe(conversationId: String): Flow<String?>
    fun getSelectedSelfId(conversationId: String): String?
    fun setSelectedSelfId(conversationId: String, selfId: String)
}

internal class ConversationSimSelectionRepositoryImpl @Inject constructor() :
    ConversationSimSelectionRepository {

    private val changes = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun observe(conversationId: String): Flow<String?> {
        return changes
            .filter { it == conversationId }
            .map { getSelectedSelfId(conversationId) }
            .onStart { emit(getSelectedSelfId(conversationId)) }
            .distinctUntilChanged()
    }

    override fun getSelectedSelfId(conversationId: String): String? {
        if (conversationId.isEmpty()) return null

        val prefs = BuglePrefs.getApplicationPrefs()
        return prefs.getString(prefKey(conversationId), null)
            ?.takeIf(String::isNotEmpty)
    }

    override fun setSelectedSelfId(conversationId: String, selfId: String) {
        if (conversationId.isEmpty() || selfId.isEmpty()) return

        val prefs = BuglePrefs.getApplicationPrefs()
        prefs.putString(prefKey(conversationId), selfId)
        changes.tryEmit(conversationId)
    }

    private fun prefKey(conversationId: String): String {
        return "$PREF_KEY_PREFIX$conversationId"
    }

    private companion object {
        const val PREF_KEY_PREFIX = "conversation_sim_selection_"
    }
}
