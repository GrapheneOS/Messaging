package com.android.messaging.ui.debug.screen.model

import androidx.compose.runtime.Immutable
import com.android.messaging.data.subscription.model.SubId

@Immutable
internal data class DebugSimUiState(
    val subId: SubId,
    val mccMnc: String,
)
