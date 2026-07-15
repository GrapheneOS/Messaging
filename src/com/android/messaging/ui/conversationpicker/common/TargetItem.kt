package com.android.messaging.ui.conversationpicker.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.common.components.selection.SelectionListAvatar
import com.android.messaging.ui.common.components.selection.SelectionListItem
import com.android.messaging.ui.common.components.selection.SelectionListItemTokens
import com.android.messaging.ui.common.components.selection.SelectionListTrailingIndicator
import com.android.messaging.ui.common.text.asLtrText
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.ui.core.MessagingPreviewColumn

private val SectionHeaderHorizontalPadding = 16.dp
private val SectionHeaderVerticalPadding = 8.dp

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
internal fun TargetItem(
    target: TargetUiState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = SelectionListItemTokens.singleShape,
) {
    val avatarContent = target.avatarContent()
    val title = target.displayName.asLtrText()
    val subtitle = target.details?.asLtrText()

    TargetRow(
        title = title,
        subtitle = subtitle,
        avatarUri = target.avatarUri,
        colorSeedCode = avatarContent.colorSeedCode,
        fallbackIcon = avatarContent.fallbackIcon,
        fallbackLabel = avatarContent.fallbackLabel,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = shape,
    )
}

@Composable
private fun TargetRow(
    title: String,
    subtitle: String?,
    avatarUri: String?,
    colorSeedCode: String?,
    fallbackIcon: ImageVector,
    fallbackLabel: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    SelectionListItem(
        modifier = modifier,
        primaryText = title,
        secondaryText = subtitle,
        isSelected = isSelected,
        enabled = true,
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = {
            SelectionListAvatar(
                avatarUri = avatarUri,
                fallbackLabel = fallbackLabel,
                colorSeedCode = colorSeedCode,
                fallbackIcon = fallbackIcon,
                isSelected = isSelected,
            )
        },
        trailingContent = {
            SelectionListTrailingIndicator(
                visible = false,
                testTag = null,
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun TargetItemPreview() {
    MessagingPreviewColumn {
        TargetItem(
            target = TargetUiState.Conversation(
                conversationId = "1",
                normalizedDestination = "+31612345678",
                displayName = "Jane Doe",
                details = "+31 6 1234 5678",
                avatarUri = null,
                isGroup = false,
            ),
            isSelected = false,
            onClick = {},
            onLongClick = null,
        )
    }
}
