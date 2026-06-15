package com.android.messaging.ui.subscription.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class SimOptionUiModel(
    val id: String,
    val label: String,
    val destination: String?,
    val slotLabel: String,
    val accentColor: Color?,
)
