package com.android.messaging.ui.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        isSystemInDarkTheme() -> dynamicDarkColorScheme(context = context)
        else -> dynamicLightColorScheme(context = context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
