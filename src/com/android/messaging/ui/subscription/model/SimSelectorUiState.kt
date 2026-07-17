package com.android.messaging.ui.subscription.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.conversation.model.ParticipantId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SimSelectorUiState(
    val options: ImmutableList<SimOptionUiModel> = persistentListOf(),
    val selectedId: ParticipantId? = null,
    val isLoading: Boolean = false,
) {
    val isAvailable: Boolean
        get() = !isLoading && options.size > 1

    val selectedOption: SimOptionUiModel?
        get() = options.firstOrNull { option -> option.id == selectedId }
}
