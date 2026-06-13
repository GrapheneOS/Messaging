package com.android.messaging.ui.core

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppColorScheme
import com.android.messaging.util.BuglePrefs

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(size = 12.dp),
    small = RoundedCornerShape(size = 16.dp),
    medium = RoundedCornerShape(size = 20.dp),
    large = RoundedCornerShape(size = 28.dp),
    extraLarge = RoundedCornerShape(size = 36.dp),
)

private val ColorSchemeSeeds = mapOf(
    AppColorScheme.GREEN to Color(0xFF689F38),
    AppColorScheme.GRAY to Color(0xFF616161),
    AppColorScheme.BLUE to Color(0xFF1976D2),
    AppColorScheme.DARK_BLUE to Color(0xFF0D47A1),
    AppColorScheme.PURPLE to Color(0xFF8E24AA),
    AppColorScheme.VIOLET to Color(0xFF5E35B1),
    AppColorScheme.TEAL to Color(0xFF00695C),
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val appColorScheme = rememberAppColorScheme(context)
    val seed = ColorSchemeSeeds[appColorScheme]
    val colorScheme = when {
        seed == null -> if (isDark) {
            dynamicDarkColorScheme(context = context)
        } else {
            dynamicLightColorScheme(context = context)
        }

        else -> {
            val base = if (isDark) darkColorScheme() else lightColorScheme()
            val onSeed = if (seed.luminance() > 0.5f) Color.Black else Color.White
            val container = lerp(
                start = seed,
                stop = if (isDark) Color.Black else Color.White,
                fraction = if (isDark) 0.6f else 0.8f,
            )
            val onContainer = if (container.luminance() > 0.5f) Color.Black else Color.White
            base.copy(
                primary = seed,
                onPrimary = onSeed,
                primaryContainer = container,
                onPrimaryContainer = onContainer,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        content = content,
    )
}

@Composable
private fun rememberAppColorScheme(context: Context): AppColorScheme = remember {
    val prefs = context.getSharedPreferences(
        BuglePrefs.SHARED_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    val colorSchemeKey = context.getString(R.string.color_scheme_pref_key)
    AppColorScheme.fromPrefValue(prefs.getString(colorSchemeKey, null))
}
