package com.android.messaging.ui.conversationpicker.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SectionHeaderHorizontalPadding = 16.dp
private val SectionHeaderVerticalPadding = 8.dp
private val ItemDividerThickness = 1.dp

@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = SectionHeaderHorizontalPadding,
                vertical = SectionHeaderVerticalPadding,
            ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun ItemDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier.padding(
            horizontal = ItemDividerHorizontalInset,
            vertical = ItemDividerThickness,
        ),
        thickness = ItemDividerThickness,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}
