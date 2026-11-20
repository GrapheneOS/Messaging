package com.android.messaging.ui.host

import android.os.UserManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.messaging.R
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlin.system.exitProcess

@Composable
internal fun DeviceOwnerSmsBlock(
    onClose: () -> Unit = { exitProcess(0) },
) {
    val context = LocalContext.current
    val userManager = remember(context) { context.getSystemService(UserManager::class.java) }
    var isSmsDisallowed by remember { mutableStateOf(false) }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        isSmsDisallowed = userManager.hasUserRestriction(UserManager.DISALLOW_SMS)
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
