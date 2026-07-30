package com.android.messaging.ui.common.components

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalIsListDetailPane = staticCompositionLocalOf { false }

internal val LocalListDetailPaneSide = staticCompositionLocalOf<ListDetailPaneSide?> { null }
