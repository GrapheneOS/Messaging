package com.android.messaging.ui.photoviewer.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R

@Composable
internal fun PhotoViewerEmptyState(modifier: Modifier) {
    PhotoViewerStatusMessage(
        modifier = modifier,
        text = stringResource(id = R.string.photo_viewer_empty),
    )
}

@Composable
internal fun PhotoViewerLoadError(modifier: Modifier) {
    PhotoViewerStatusMessage(
        modifier = modifier,
        text = stringResource(id = R.string.photo_viewer_load_error),
    )
}

@Composable
private fun PhotoViewerStatusMessage(
    modifier: Modifier,
    text: String,
) {
    Column(
        modifier = modifier.padding(all = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = null,
            modifier = Modifier.size(size = 40.dp),
            tint = MaterialTheme.colorScheme.inverseOnSurface,
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
