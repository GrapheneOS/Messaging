package com.android.messaging.ui.conversation.metadata.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.android.messaging.ui.common.text.asLtrText
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import kotlinx.collections.immutable.ImmutableList

internal fun phoneNumberCopyAction(
    targets: ImmutableList<ConversationMetadataUiState.PhoneNumberCopyTarget>,
    onPhoneNumberCopy: (String) -> Unit,
    onShowMenu: () -> Unit,
): (() -> Unit)? {
    return when (targets.size) {
        0 -> null
        1 -> {
            { onPhoneNumberCopy(targets.single().phoneNumber) }
        }
        else -> onShowMenu
    }
}

@Composable
internal fun ConversationPhoneNumberCopyMenu(
    expanded: Boolean,
    targets: ImmutableList<ConversationMetadataUiState.PhoneNumberCopyTarget>,
    onDismissRequest: () -> Unit,
    onPhoneNumberCopy: (String) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        targets.forEach { target ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(text = target.displayName)
                        if (target.displayName != target.phoneNumber) {
                            Text(
                                text = target.phoneNumber.asLtrText(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                onClick = { onPhoneNumberCopy(target.phoneNumber) },
            )
        }
    }
}
