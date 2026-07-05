package com.android.messaging.ui.vcarddetail.common

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardField
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.ui.common.components.attachment.VCardAvatar
import com.android.messaging.ui.common.components.selection.SelectionListItemTokens
import com.android.messaging.ui.core.MessagingPreviewColumn
import kotlinx.collections.immutable.persistentListOf

private val RowHorizontalPadding = SelectionListItemTokens.rowHorizontalPadding
private val RowVerticalPadding = SelectionListItemTokens.rowVerticalPadding
private val AvatarToTextSpacing = SelectionListItemTokens.avatarToTextSpacing
private val AvatarSize = 48.dp
private val AvatarIconSize = 24.dp

@Composable
internal fun VCardContactCard(
    contact: VCardContact,
    onFieldClick: (VCardFieldAction) -> Unit,
    onFieldLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SelectionListItemTokens.singleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        VCardContactHeader(
            displayName = contact.displayName,
            avatarUri = contact.avatarUri,
        )

        contact.fields.forEach { field ->
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = RowHorizontalPadding),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            )

            VCardFieldRow(
                field = field,
                onClick = { onFieldClick(field.action) },
                onLongClick = { onFieldLongClick(field.value) },
            )
        }
    }
}

@Composable
private fun VCardContactHeader(
    displayName: String?,
    avatarUri: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = RowHorizontalPadding,
                vertical = RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VCardAvatar(
            avatarUri = avatarUri,
            avatarName = displayName,
            size = AvatarSize,
            iconSize = AvatarIconSize,
        )

        Spacer(modifier = Modifier.width(AvatarToTextSpacing))

        Text(
            text = displayName.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VCardFieldRow(
    field: VCardField,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(
                horizontal = RowHorizontalPadding,
                vertical = RowVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = field.value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val label = field.label
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VCardContactCardPreview() {
    MessagingPreviewColumn {
        VCardContactCard(
            contact = VCardContact(
                displayName = "Ada Lovelace",
                avatarUri = null,
                fields = persistentListOf(
                    VCardField(
                        value = "+1 555 0001",
                        label = "Mobile",
                        action = VCardFieldAction.Dial("+15550001"),
                    ),
                    VCardField(
                        value = "ada@example.com",
                        label = "Home",
                        action = VCardFieldAction.Email("ada@example.com"),
                    ),
                    VCardField(
                        value = "1 Analytical Engine Way, London",
                        label = "Address",
                        action = VCardFieldAction.OpenMap("1 Analytical Engine Way, London"),
                    ),
                    VCardField(
                        value = "First computer programmer",
                        label = stringResource(R.string.vcard_detail_notes_label),
                        action = VCardFieldAction.None,
                    ),
                ),
            ),
            onFieldClick = {},
            onFieldLongClick = {},
        )
    }
}
