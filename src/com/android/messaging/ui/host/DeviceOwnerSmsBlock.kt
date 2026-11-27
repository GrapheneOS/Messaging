package com.android.messaging.ui.host

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.R
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlin.system.exitProcess

@Composable
internal fun DeviceOwnerSmsBlock(
    model: DeviceOwnerSmsBlockModel,
    onClose: () -> Unit = { exitProcess(0) },
) {
    val isSmsDisallowed by model.isSmsDisallowed.collectAsStateWithLifecycle()

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        model.onHostResumed()
    }

    if (isSmsDisallowed) {
        DeviceOwnerSmsBlockDialog(onClose = onClose)
    }
}

@Composable
internal fun DeviceOwnerSmsBlockDialog(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = {},
        text = {
            Text(text = stringResource(R.string.requires_sms_permissions_message))
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.requires_sms_permissions_close_button))
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun DeviceOwnerSmsBlockDialogPreview() {
    MessagingPreviewTheme {
        DeviceOwnerSmsBlockDialog(onClose = {})
    }
}
