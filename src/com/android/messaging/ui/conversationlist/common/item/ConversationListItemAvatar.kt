package com.android.messaging.ui.conversationlist.common.item

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import com.android.messaging.R
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.ui.common.components.participant.ParticipantQuickActionsPopup
import com.android.messaging.ui.common.components.participant.PhoneNumberCopyTarget
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.common.components.selection.SelectionListAvatar
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ConversationListItemAvatar(
    item: ConversationListItemUiModel,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    phoneNumberCopyTargets: ImmutableList<PhoneNumberCopyTarget>,
    onQuickActionsOpen: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onInfoClick: () -> Unit,
) {
    val fallbackIcon = when {
        item.avatar.isGroup -> Icons.Default.Group
        else -> Icons.Default.Person
    }

    val fallbackLabel = when {
        item.avatar.isGroup -> null
        else -> participantAvatarLabel(source = item.title)
    }

    val colorSeedCode = participantColorSeed(
        normalizedDestination = item.avatar.normalizedDestination,
    )

    var showQuickActions by remember { mutableStateOf(false) }
    val onAvatarClick = {
        when {
            isSelectionMode -> onToggleSelection()
            else -> {
                onQuickActionsOpen()
                showQuickActions = true
            }
        }
    }
    val avatarContentDescription = stringResource(
        when {
            isSelectionMode -> R.string.conversation_list_toggle_selection
            else -> R.string.conversation_list_show_conversation_actions
        },
        item.title.orEmpty(),
    )
    val fallbackCopyTarget = item.avatar.normalizedDestination
        ?.takeIf(String::isNotBlank)
        ?.takeIf(MmsSmsUtils::isPhoneNumber)
        ?.let { phoneNumber ->
            PhoneNumberCopyTarget(
                displayName = item.title?.takeIf(String::isNotBlank) ?: phoneNumber,
                phoneNumber = phoneNumber,
            )
        }
    val resolvedCopyTargets = phoneNumberCopyTargets.takeIf { it.isNotEmpty() }
        ?: fallbackCopyTarget?.let { persistentListOf(it) }
        ?: persistentListOf()
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(ClipboardManager::class.java)
    }

    Box(modifier = Modifier.size(ItemAvatarSize)) {
        SelectionListAvatar(
            avatarUri = item.avatar.uri,
            size = ItemAvatarSize,
            fallbackLabel = fallbackLabel,
            colorSeedCode = colorSeedCode,
            fallbackIcon = fallbackIcon,
            isSelected = item.isSelected,
            modifier = Modifier
                .clip(CircleShape)
                .conversationListAvatarClickSemantics(
                    contentDescription = avatarContentDescription,
                    onClick = onAvatarClick,
                ),
        )

        ConversationListAvatarQuickActions(
            item = item,
            visible = showQuickActions && !isSelectionMode,
            fallbackIcon = fallbackIcon,
            fallbackLabel = fallbackLabel,
            colorSeedCode = colorSeedCode,
            phoneNumberCopyTargets = resolvedCopyTargets,
            onPhoneNumberCopy = { phoneNumber ->
                clipboardManager?.setPrimaryClip(
                    ClipData.newPlainText(null, phoneNumber),
                )
            },
            onDismiss = { showQuickActions = false },
            onMessageClick = onMessageClick,
            onCallClick = onCallClick,
            onContactClick = onContactClick,
            onInfoClick = onInfoClick,
        )
    }
}

private fun Modifier.conversationListAvatarClickSemantics(
    contentDescription: String,
    onClick: () -> Unit,
): Modifier {
    return clickable(onClick = onClick)
        .clearAndSetSemantics {
            this.contentDescription = contentDescription
            role = Role.Button
            onClick(label = contentDescription) {
                onClick()
                true
            }
        }
}

@Composable
private fun ConversationListAvatarQuickActions(
    item: ConversationListItemUiModel,
    visible: Boolean,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    colorSeedCode: String?,
    phoneNumberCopyTargets: ImmutableList<PhoneNumberCopyTarget>,
    onPhoneNumberCopy: (String) -> Unit,
    onDismiss: () -> Unit,
    onMessageClick: () -> Unit,
    onCallClick: (() -> Unit)?,
    onContactClick: (() -> Unit)?,
    onInfoClick: () -> Unit,
) {
    ParticipantQuickActionsPopup(
        visible = visible,
        avatarUri = item.avatar.uri,
        displayName = item.title.orEmpty(),
        subtitle = item.avatar.subtitle,
        fallbackIcon = fallbackIcon,
        fallbackLabel = fallbackLabel,
        colorSeedCode = colorSeedCode,
        onDismiss = onDismiss,
        onMessageClick = {
            onMessageClick()
            onDismiss()
        },
        onCallClick = {
            onCallClick?.invoke()
            onDismiss()
        }.takeIf { item.avatar.canCall && onCallClick != null },
        onContactClick = {
            onContactClick?.invoke()
            onDismiss()
        }.takeIf { item.avatar.canShowContact && onContactClick != null },
        onInfoClick = {
            onInfoClick()
            onDismiss()
        },
        isContactSaved = item.avatar.isContactSaved,
        phoneNumberCopyTargets = phoneNumberCopyTargets,
        onPhoneNumberCopy = { phoneNumber ->
            onPhoneNumberCopy(phoneNumber)
            onDismiss()
        },
    )
}
