package com.android.messaging.ui.common.components.selection

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
internal data class SelectionListItemColors(
    val containerColor: Color,
    val selectedContainerColor: Color,
    val primaryTextColor: Color,
    val selectedPrimaryTextColor: Color,
    val secondaryTextColor: Color,
    val selectedSecondaryTextColor: Color,
)

internal val LocalSelectionListItemColors = compositionLocalOf<SelectionListItemColors?> { null }

@Composable
@ReadOnlyComposable
internal fun selectionListItemColors(
    containerColor: Color = MaterialTheme.colorScheme.background,
    selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    selectedPrimaryTextColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedSecondaryTextColor: Color = MaterialTheme.colorScheme.onSecondaryContainer.copy(
        alpha = 0.7f,
    ),
): SelectionListItemColors {
    return SelectionListItemColors(
        containerColor = containerColor,
        selectedContainerColor = selectedContainerColor,
        primaryTextColor = primaryTextColor,
        selectedPrimaryTextColor = selectedPrimaryTextColor,
        secondaryTextColor = secondaryTextColor,
        selectedSecondaryTextColor = selectedSecondaryTextColor,
    )
}

@Composable
@ReadOnlyComposable
internal fun currentSelectionListItemColors(): SelectionListItemColors {
    return LocalSelectionListItemColors.current ?: selectionListItemColors()
}
