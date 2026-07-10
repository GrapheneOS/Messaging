package com.android.messaging.ui.debug.screen.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface MmsConfigItemUiState {
    val key: String

    data class Toggle(
        override val key: String,
        val checked: Boolean,
    ) : MmsConfigItemUiState

    data class Editable(
        override val key: String,
        val value: String,
        val isNumeric: Boolean,
    ) : MmsConfigItemUiState
}
