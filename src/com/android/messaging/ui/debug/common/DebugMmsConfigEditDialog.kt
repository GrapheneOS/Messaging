package com.android.messaging.ui.debug.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.android.messaging.ui.debug.screen.model.MmsConfigItemUiState

@Composable
internal fun DebugMmsConfigEditDialog(
    item: MmsConfigItemUiState.Editable,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(item.key) { mutableStateOf(item.value) }

    val keyboardType = when {
        item.isNumeric -> KeyboardType.Phone
        else -> KeyboardType.Text
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = item.key)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}
