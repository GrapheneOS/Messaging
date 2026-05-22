package com.android.messaging.ui.blockedparticipants.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.android.messaging.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockedParticipantsTopAppBar(
    onNavigateBack: () -> Unit,
    selectedCount: Int = 0,
    onClearSelectionClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    val isSelectionMode = selectedCount > 0

    TopAppBar(
        title = {
            Text(
                text = when {
                    isSelectionMode -> stringResource(
                        R.string.blocked_contacts_selection_title,
                        selectedCount,
                    )
                    else -> stringResource(R.string.blocked_contacts_title)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onClearSelectionClick) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(
                            R.string.blocked_contacts_clear_selection_content_description,
                        ),
                    )
                }
            } else {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
