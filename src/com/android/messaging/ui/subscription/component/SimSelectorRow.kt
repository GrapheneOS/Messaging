package com.android.messaging.ui.subscription.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.subscription.model.SimOptionUiModel
import com.android.messaging.ui.subscription.model.SimSelectorUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val ChipAvatarSize = 24.dp
private val DropdownAvatarSize = 32.dp

@Composable
internal fun SimSelectorRow(
    uiState: SimSelectorUiState,
    prefixText: String,
    chipContentDescription: String,
    selectedContentDescription: String,
    onSimSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showDestination: Boolean = false,
) {
    if (!uiState.isAvailable) {
        return
    }

    val selectedOption = uiState.selectedOption ?: return

    var isDropdownExpanded by rememberSaveable { mutableStateOf(value = false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Text(
            text = prefixText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box {
            SimSelectorChip(
                option = selectedOption,
                contentDescription = chipContentDescription,
                showDestination = showDestination,
                onClick = { isDropdownExpanded = true },
            )

            SimSelectorDropdown(
                expanded = isDropdownExpanded,
                options = uiState.options,
                selectedId = selectedOption.id,
                selectedContentDescription = selectedContentDescription,
                onSimSelected = onSimSelected,
                onDismissRequest = { isDropdownExpanded = false },
            )
        }
    }
}

@Composable
private fun SimSelectorChip(
    option: SimOptionUiModel,
    contentDescription: String,
    showDestination: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipText = when {
        showDestination -> option.destination ?: option.label
        else -> option.label
    }

    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag = SIM_SELECTOR_CHIP_TEST_TAG)
            .semantics { this.contentDescription = contentDescription }
            .padding(
                start = 6.dp,
                end = 8.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        SimAvatar(
            slotLabel = option.slotLabel,
            accentColor = option.accentColor,
            size = ChipAvatarSize,
        )

        Text(
            text = chipText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Icon(
            imageVector = Icons.Rounded.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SimSelectorDropdown(
    expanded: Boolean,
    options: ImmutableList<SimOptionUiModel>,
    selectedId: String,
    selectedContentDescription: String,
    onSimSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        modifier = modifier
            .testTag(tag = SIM_SELECTOR_DROPDOWN_TEST_TAG),
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        options.forEach { option ->
            SimSelectorDropdownItem(
                option = option,
                isSelected = option.id == selectedId,
                selectedContentDescription = selectedContentDescription,
                onClick = {
                    onSimSelected(option.id)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
private fun SimSelectorDropdownItem(
    option: SimOptionUiModel,
    isSelected: Boolean,
    selectedContentDescription: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier
            .testTag(tag = simSelectorItemTestTag(id = option.id)),
        onClick = onClick,
        leadingIcon = {
            SimAvatar(
                slotLabel = option.slotLabel,
                accentColor = option.accentColor,
                size = DropdownAvatarSize,
            )
        },
        text = {
            SimSelectorDropdownItemText(
                label = option.label,
                destination = option.destination,
            )
        },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = selectedContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}

@Composable
private fun SimSelectorDropdownItemText(
    label: String,
    destination: String?,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        destination?.let {
            Text(
                text = destination,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun previewSimSelectorUiState(): SimSelectorUiState {
    val options = persistentListOf(
        SimOptionUiModel(
            id = "self_1",
            label = "SIM 1",
            destination = "+31 6 1234 5678",
            slotLabel = "1",
            accentColor = null,
        ),
        SimOptionUiModel(
            id = "self_2",
            label = "Work",
            destination = "+31 6 8765 4321",
            slotLabel = "2",
            accentColor = Color(color = 0xFF2E7D32),
        ),
    )

    return SimSelectorUiState(
        options = options,
        selectedId = "self_1",
    )
}

@PreviewLightDark
@Composable
private fun SimSelectorRowPreview() {
    MessagingPreviewColumn {
        SimSelectorRow(
            uiState = previewSimSelectorUiState(),
            prefixText = "SIM:",
            chipContentDescription = "Selected SIM",
            selectedContentDescription = "Selected",
            onSimSelected = { _ -> },
        )
    }
}
