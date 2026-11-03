package com.android.messaging.ui.conversation.links.delegate

import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.data.appsettings.model.LinkOpeningMode
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.conversation.common.ConversationScreenDelegate
import com.android.messaging.ui.conversation.links.model.ConversationLinkConfirmationUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

internal interface ConversationLinkActions {
    fun onExternalUriClicked(uri: String)
    fun confirmOpenLink(confirmation: ConversationLinkConfirmationUiState)
    fun dismissOpenLinkConfirmation()
}

internal interface ConversationLinkDelegate :
    ConversationScreenDelegate<ConversationLinkConfirmationUiState?>,
    ConversationLinkActions {
    val effects: Flow<ConversationScreenEffect>
}

internal class ConversationLinkDelegateImpl @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ConversationLinkDelegate {

    private val _effects = MutableSharedFlow<ConversationScreenEffect>(extraBufferCapacity = 1)
    private val _state = MutableStateFlow<ConversationLinkConfirmationUiState?>(value = null)

    // A null uri cancels a click still waiting for the setting.
    private val clickedUris = MutableSharedFlow<String?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val effects = _effects.asSharedFlow()
    override val state = _state.asStateFlow()

    private var boundScope: CoroutineScope? = null

    override fun bind(
        scope: CoroutineScope,
        conversationIdFlow: StateFlow<ConversationId?>,
    ) {
        if (boundScope != null) {
            return
        }

        boundScope = scope

        scope.launch(defaultDispatcher) {
            merge(conversationIdFlow.map { null }, clickedUris).collectLatest { uri ->
                _state.value = null
                uri?.let { handleClick(uri = it) }
            }
        }
    }

    override fun onExternalUriClicked(uri: String) {
        clickedUris.tryEmit(uri)
    }

    override fun confirmOpenLink(confirmation: ConversationLinkConfirmationUiState) {
        if (_state.compareAndSet(expect = confirmation, update = null)) {
            boundScope?.launch(defaultDispatcher) {
                openUri(uri = confirmation.uri)
            }
        }
    }

    override fun dismissOpenLinkConfirmation() {
        _state.value = null
        clickedUris.tryEmit(null)
    }

    private suspend fun handleClick(uri: String) {
        when (appSettingsRepository.getLinkOpeningMode()) {
            LinkOpeningMode.AskBeforeOpening -> {
                _state.value = linkConfirmationUiState(uri = uri)
            }

            LinkOpeningMode.OpenOnTap -> openUri(uri = uri)
            LinkOpeningMode.DoNotOpen -> Unit
        }
    }

    private suspend fun openUri(uri: String) {
        _effects.emit(ConversationScreenEffect.OpenExternalUri(uri = uri))
    }

    private fun linkConfirmationUiState(uri: String): ConversationLinkConfirmationUiState {
        val parsedUri = uri.toUri()
        val host = parsedUri.host?.takeIf { it.isNotBlank() }

        return ConversationLinkConfirmationUiState(
            uri = uri,
            destination = host ?: parsedUri.readableTarget(),
            showsFullUri = host != null,
        )
    }

    private fun Uri.readableTarget(): String {
        val target = schemeSpecificPart.orEmpty()

        val readableTarget = when (scheme?.lowercase()) {
            GEO_SCHEME -> target.substringAfter(delimiter = GEO_QUERY_PREFIX)
            else -> target
        }

        return readableTarget.ifBlank { toString() }
    }

    private companion object {
        private const val GEO_SCHEME = "geo"
        private const val GEO_QUERY_PREFIX = "?q="
    }
}
