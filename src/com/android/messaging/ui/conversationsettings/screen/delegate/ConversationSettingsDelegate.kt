package com.android.messaging.ui.conversationsettings.screen.delegate

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.action.BugleActionToasts
import com.android.messaging.datamodel.action.UpdateDestinationBlockedAction
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsScreenDelegate
import com.android.messaging.ui.conversationsettings.screen.mapper.ConversationSettingsUiStateMapper
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface ConversationSettingsDelegate :
    ConversationSettingsScreenDelegate<ConversationSettingsUiState> {
    fun setConversationId(conversationId: String)
    fun setDestinationBlocked(blocked: Boolean)
}

internal class ConversationSettingsDelegateImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val mapper: ConversationSettingsUiStateMapper,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ConversationSettingsDelegate {

    private val _state = MutableStateFlow(ConversationSettingsUiState())
    override val state: StateFlow<ConversationSettingsUiState> = _state.asStateFlow()

    private val refreshTriggers: Channel<Unit> = Channel(Channel.CONFLATED)

    private var conversationId: String = ""
    private var isBound = false

    override fun setConversationId(conversationId: String) {
        this.conversationId = conversationId
    }

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        scope.launch {
            merge(
                conversationChangesFlow(),
                refreshTriggers.receiveAsFlow(),
            )
                .onStart { emit(Unit) }
                .conflate()
                .map { mapper.map(conversationId) }
                .flowOn(defaultDispatcher)
                .collect { _state.value = it }
        }
    }

    override fun refresh() {
        refreshTriggers.trySend(Unit)
    }

    override fun setDestinationBlocked(blocked: Boolean) {
        val participant: ParticipantUiState = _state.value.otherParticipant ?: return

        UpdateDestinationBlockedAction.updateDestinationBlocked(
            participant.normalizedDestination,
            blocked,
            conversationId,
            BugleActionToasts.makeUpdateDestinationBlockedActionListener(context),
        )
    }

    private fun conversationChangesFlow(): Flow<Unit> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
            val metadataUri = MessagingContentProvider.buildConversationMetadataUri(
                conversationId
            )
            val participantsUri = MessagingContentProvider.buildConversationParticipantsUri(
                conversationId
            )
            contentResolver.registerContentObserver(metadataUri, false, observer)
            contentResolver.registerContentObserver(participantsUri, false, observer)
            awaitClose {
                contentResolver.unregisterContentObserver(observer)
            }
        }
    }
}
