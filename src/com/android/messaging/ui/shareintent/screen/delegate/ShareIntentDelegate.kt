package com.android.messaging.ui.shareintent.screen.delegate

import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.ui.shareintent.screen.model.ShareIntentUiState as State
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface ShareIntentScreenDelegate {
    val state: StateFlow<State>
    fun bind(scope: CoroutineScope)
}

internal class ShareIntentScreenDelegateImpl @Inject constructor(
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ShareIntentScreenDelegate {

    private val _state = MutableStateFlow(State())
    override val state: StateFlow<State> = _state.asStateFlow()

    private var isBound = false

    override fun bind(scope: CoroutineScope) {
        if (isBound) return
        isBound = true

        scope.launch(defaultDispatcher) {
        }
    }
}
