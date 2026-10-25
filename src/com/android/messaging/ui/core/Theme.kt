package com.android.messaging.ui.core

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(size = 12.dp),
    small = RoundedCornerShape(size = 16.dp),
    medium = RoundedCornerShape(size = 20.dp),
    large = RoundedCornerShape(size = 28.dp),
    extraLarge = RoundedCornerShape(size = 36.dp),
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = appColorScheme(
        context = LocalContext.current,
        isDarkTheme = isSystemInDarkTheme(),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content,
    )
}

internal fun appColorScheme(context: Context, isDarkTheme: Boolean): ColorScheme {
    return when {
        isDarkTheme -> dynamicDarkColorScheme(context = context)
        else -> dynamicLightColorScheme(context = context)
    }
}
