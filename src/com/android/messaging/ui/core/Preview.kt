package com.android.messaging.ui.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MessagingPreviewTheme(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppTheme {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}

@Composable
internal fun MessagingPreviewColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MessagingPreviewTheme(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
        ) {
            content()
        }
    }
}
