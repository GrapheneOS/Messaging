package com.android.messaging.ui.conversationlist.ui.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.messaging.R

private val OverflowMenuWidth = 220.dp

@Composable
internal fun OverflowMenu(
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.more_options),
            )
        }

        DropdownMenu(
            modifier = Modifier.width(OverflowMenuWidth),
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            content { isExpanded = false }
        }
    }
}

@Composable
internal fun OverflowMenuItem(
    labelResId: Int,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(text = stringResource(labelResId))
        },
        onClick = onClick,
    )
}
