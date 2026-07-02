package com.android.messaging.ui.conversation.metadata.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import com.android.messaging.R
import com.android.messaging.ui.common.components.participant.ParticipantAvatar
import com.android.messaging.ui.common.components.participant.participantAvatarLabel
import com.android.messaging.ui.common.components.participant.participantColorSeed
import com.android.messaging.ui.conversation.CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_ARCHIVE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_CALL_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_OVERFLOW_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SHOW_SUBJECT_FIELD_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.preview.previewGroupMetadata
import com.android.messaging.ui.conversation.preview.previewMetadata
import com.android.messaging.ui.conversation.preview.previewSimSelectorUiState
import com.android.messaging.ui.core.MessagingPreviewColumn
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.messaging.ui.subscription.mapper.resolveDisplayName
import com.android.messaging.util.AccessibilityUtil

private val CONVERSATION_TOP_APP_BAR_TITLE_SPACING = 12.dp
private val CONVERSATION_TOP_APP_BAR_AVATAR_SIZE = 36.dp
private val CONVERSATION_TOP_APP_BAR_AVATAR_FALLBACK_SIZE = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationTopAppBar(
    modifier: Modifier = Modifier,
    metadata: ConversationMetadataUiState,
    isAddPeopleVisible: Boolean = false,
    isCallVisible: Boolean = false,
    isArchiveVisible: Boolean = false,
    isUnarchiveVisible: Boolean = false,
    isAddContactVisible: Boolean = false,
    isDeleteConversationVisible: Boolean = false,
    isShowSubjectFieldVisible: Boolean = false,
    simSelector: ConversationSimSelectorUiState = ConversationSimSelectorUiState(),
    onAddPeopleClick: () -> Unit,
    onCallClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onUnarchiveClick: () -> Unit = {},
    onAddContactClick: () -> Unit = {},
    onDeleteConversationClick: () -> Unit = {},
    onShowSubjectFieldClick: () -> Unit = {},
    onSimSelectorClick: () -> Unit = {},
    onTitleClick: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val presentation = rememberConversationTopAppBarPresentation(
        metadata = metadata,
    )
    val isTitleClickable = metadata is ConversationMetadataUiState.Present
    val overflowVisibility = ConversationTopAppBarOverflowVisibility(
        isAddPeopleVisible = isAddPeopleVisible,
        isArchiveVisible = isArchiveVisible,
        isUnarchiveVisible = isUnarchiveVisible,
        isAddContactVisible = isAddContactVisible,
        isDeleteConversationVisible = isDeleteConversationVisible,
        isShowSubjectFieldVisible = isShowSubjectFieldVisible,
        isSimSelectorVisible = simSelector.isAvailable,
    )

    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        title = {
            ConversationTopAppBarTitle(
                isClickable = isTitleClickable,
                onClick = onTitleClick,
                presentation = presentation,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                )
            }
        },
        actions = {
            ConversationTopAppBarActions(
                isCallVisible = isCallVisible,
                overflowVisibility = overflowVisibility,
                simSelectorLabel = simSelector.selectedSubscription
                    ?.label
                    ?.resolveDisplayName()
                    .orEmpty(),
                onCallClick = onCallClick,
                onAddPeopleClick = onAddPeopleClick,
                onArchiveClick = onArchiveClick,
                onUnarchiveClick = onUnarchiveClick,
                onAddContactClick = onAddContactClick,
                onDeleteConversationClick = onDeleteConversationClick,
                onShowSubjectFieldClick = onShowSubjectFieldClick,
                onSimSelectorClick = onSimSelectorClick,
            )
        },
    )
}

@Composable
private fun rememberConversationTopAppBarPresentation(
    metadata: ConversationMetadataUiState,
): ConversationTopAppBarPresentation {
    val title = conversationTitle(metadata)
    val subtitle = conversationSubtitle(metadata)
    val subtitleContentDescription = conversationSubtitleContentDescription(
        metadata = metadata,
    )

    val avatar = conversationAvatar(metadata)
    val isBlocked = metadata is ConversationMetadataUiState.Present && metadata.isBlocked

    return remember(
        metadata,
        title,
        subtitle,
        subtitleContentDescription,
        avatar,
        isBlocked,
    ) {
        ConversationTopAppBarPresentation(
            title = title,
            subtitle = subtitle,
            subtitleContentDescription = subtitleContentDescription,
            avatar = avatar,
            isBlocked = isBlocked,
        )
    }
}

@Composable
private fun ConversationTopAppBarTitle(
    isClickable: Boolean,
    onClick: () -> Unit,
    presentation: ConversationTopAppBarPresentation,
) {
    Row(
        modifier = Modifier
            .heightIn(min = TopAppBarDefaults.TopAppBarExpandedHeight)
            .testTag(tag = CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG)
            .clickable(
                enabled = isClickable,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = CONVERSATION_TOP_APP_BAR_TITLE_SPACING,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationAvatar(
            avatar = presentation.avatar,
            isBlocked = presentation.isBlocked,
        )

        ConversationTopAppBarText(
            presentation = presentation,
        )
    }
}

@Composable
private fun ConversationTopAppBarText(
    presentation: ConversationTopAppBarPresentation,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = presentation.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (presentation.subtitle != null) {
            Text(
                modifier = Modifier.semantics {
                    presentation.subtitleContentDescription?.let { subtitleContentDescription ->
                        contentDescription = subtitleContentDescription
                    }
                },
                text = presentation.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConversationTopAppBarActions(
    isCallVisible: Boolean,
    overflowVisibility: ConversationTopAppBarOverflowVisibility,
    simSelectorLabel: String,
    onCallClick: () -> Unit,
    onAddPeopleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onDeleteConversationClick: () -> Unit,
    onShowSubjectFieldClick: () -> Unit,
    onSimSelectorClick: () -> Unit,
) {
    if (isCallVisible) {
        IconButton(
            modifier = Modifier.testTag(CONVERSATION_CALL_BUTTON_TEST_TAG),
            onClick = onCallClick,
        ) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = stringResource(id = R.string.action_call),
            )
        }
    }

    if (overflowVisibility.isOverflowVisible) {
        ConversationTopAppBarOverflowMenu(
            visibility = overflowVisibility,
            simSelectorLabel = simSelectorLabel,
            onAddPeopleClick = onAddPeopleClick,
            onArchiveClick = onArchiveClick,
            onUnarchiveClick = onUnarchiveClick,
            onAddContactClick = onAddContactClick,
            onDeleteConversationClick = onDeleteConversationClick,
            onShowSubjectFieldClick = onShowSubjectFieldClick,
            onSimSelectorClick = onSimSelectorClick,
        )
    }
}

@Composable
private fun ConversationTopAppBarOverflowMenu(
    visibility: ConversationTopAppBarOverflowVisibility,
    simSelectorLabel: String,
    onAddPeopleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onDeleteConversationClick: () -> Unit,
    onShowSubjectFieldClick: () -> Unit,
    onSimSelectorClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(value = false) }

    IconButton(
        modifier = Modifier.testTag(CONVERSATION_OVERFLOW_BUTTON_TEST_TAG),
        onClick = { isExpanded = true },
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(id = R.string.action_more_options),
        )
    }

    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
    ) {
        ConversationTopAppBarOverflowMenuContent(
            visibility = visibility,
            simSelectorLabel = simSelectorLabel,
            onAddPeopleClick = onAddPeopleClick,
            onArchiveClick = onArchiveClick,
            onUnarchiveClick = onUnarchiveClick,
            onAddContactClick = onAddContactClick,
            onDeleteConversationClick = onDeleteConversationClick,
            onShowSubjectFieldClick = onShowSubjectFieldClick,
            onSimSelectorClick = onSimSelectorClick,
            onItemClick = { action ->
                isExpanded = false
                action()
            },
        )
    }
}

@Composable
private fun ConversationTopAppBarOverflowMenuContent(
    visibility: ConversationTopAppBarOverflowVisibility,
    simSelectorLabel: String,
    onAddPeopleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onDeleteConversationClick: () -> Unit,
    onShowSubjectFieldClick: () -> Unit,
    onSimSelectorClick: () -> Unit,
    onItemClick: (() -> Unit) -> Unit,
) {
    ConversationTopAppBarOverflowMenuItem(
        isVisible = visibility.isSimSelectorVisible,
        testTag = CONVERSATION_SIM_SELECTOR_MENU_ITEM_TEST_TAG,
        label = stringResource(R.string.conversation_switch_sims),
        secondaryLabel = simSelectorLabel,
        icon = Icons.Rounded.SimCard,
        onClick = { onItemClick(onSimSelectorClick) },
    )

    ConversationTopAppBarOverflowMenuItem(
        isVisible = visibility.isAddPeopleVisible,
        testTag = CONVERSATION_ADD_PEOPLE_BUTTON_TEST_TAG,
        label = stringResource(id = R.string.conversation_add_people),
        icon = Icons.Rounded.GroupAdd,
        onClick = { onItemClick(onAddPeopleClick) },
    )

    ConversationTopAppBarOverflowMenuItem(
        isVisible = visibility.isAddContactVisible,
        testTag = CONVERSATION_ADD_CONTACT_BUTTON_TEST_TAG,
        label = stringResource(id = R.string.action_add_contact),
        icon = Icons.Rounded.PersonAdd,
        onClick = { onItemClick(onAddContactClick) },
    )

    ConversationTopAppBarOverflowMenuItem(
        isVisible = visibility.isShowSubjectFieldVisible,
        testTag = CONVERSATION_SHOW_SUBJECT_FIELD_MENU_ITEM_TEST_TAG,
        label = stringResource(id = R.string.conversation_show_subject_field),
        icon = Icons.AutoMirrored.Rounded.Subject,
        onClick = { onItemClick(onShowSubjectFieldClick) },
    )

    ConversationTopAppBarOverflowMenuItem(
        isVisible = visibility.isArchiveVisible,
        testTag = CONVERSATION_ARCHIVE_BUTTON_TEST_TAG,
        label = stringResource(id = R.string.action_archive),
        icon = Icons.Rounded.Archive,
        onClick = { onItemClick(onArchiveClick) },
    )

    ConversationTopAppBarOverflowMenuItem(
        isVisible = visibility.isUnarchiveVisible,
        testTag = CONVERSATION_UNARCHIVE_BUTTON_TEST_TAG,
        label = stringResource(id = R.string.action_unarchive),
        icon = Icons.Rounded.Unarchive,
        onClick = { onItemClick(onUnarchiveClick) },
    )

    ConversationTopAppBarOverflowMenuItem(
        isVisible = visibility.isDeleteConversationVisible,
        testTag = CONVERSATION_DELETE_CONVERSATION_BUTTON_TEST_TAG,
        label = stringResource(id = R.string.action_delete),
        icon = Icons.Rounded.Delete,
        onClick = { onItemClick(onDeleteConversationClick) },
    )
}

@Composable
private fun ConversationTopAppBarOverflowMenuItem(
    isVisible: Boolean,
    testTag: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    secondaryLabel: String? = null,
) {
    if (!isVisible) {
        return
    }

    DropdownMenuItem(
        modifier = Modifier.testTag(tag = testTag),
        text = {
            when {
                secondaryLabel.isNullOrEmpty() -> {
                    Text(text = label)
                }

                else -> {
                    Column {
                        Text(text = label)
                        Text(
                            text = secondaryLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ConversationAvatar(
    avatar: ConversationMetadataUiState.Avatar,
    isBlocked: Boolean,
) {
    when (avatar) {
        ConversationMetadataUiState.Avatar.Group -> {
            ParticipantAvatar(
                avatarUri = null,
                size = CONVERSATION_TOP_APP_BAR_AVATAR_SIZE,
                fallbackLabel = null,
                fallbackSize = CONVERSATION_TOP_APP_BAR_AVATAR_FALLBACK_SIZE,
                fallbackIcon = Icons.Rounded.Group,
            )
        }

        is ConversationMetadataUiState.Avatar.Single -> {
            ParticipantAvatar(
                avatarUri = avatar.photoUri.takeUnless { isBlocked },
                size = CONVERSATION_TOP_APP_BAR_AVATAR_SIZE,
                fallbackLabel = participantAvatarLabel(source = avatar.displayName)
                    .takeUnless { isBlocked },
                colorSeedCode = participantColorSeed(
                    normalizedDestination = avatar.normalizedDestination,
                ),
                fallbackSize = CONVERSATION_TOP_APP_BAR_AVATAR_FALLBACK_SIZE,
                fallbackIcon = when {
                    isBlocked -> Icons.Default.Block
                    else -> Icons.Rounded.Person
                },
            )
        }
    }
}

private fun conversationAvatar(
    metadata: ConversationMetadataUiState,
): ConversationMetadataUiState.Avatar {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> {
            ConversationMetadataUiState.Avatar.Single(
                photoUri = null,
                normalizedDestination = null,
                displayName = null,
            )
        }

        ConversationMetadataUiState.Unavailable -> {
            ConversationMetadataUiState.Avatar.Single(
                photoUri = null,
                normalizedDestination = null,
                displayName = null,
            )
        }

        is ConversationMetadataUiState.Present -> metadata.avatar
    }
}

@Composable
private fun conversationTitle(
    metadata: ConversationMetadataUiState,
): String {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> stringResource(id = R.string.app_name)

        ConversationMetadataUiState.Unavailable -> stringResource(id = R.string.app_name)

        is ConversationMetadataUiState.Present -> {
            metadata
                .title
                .takeIf { it.isNotBlank() }
                ?: stringResource(id = R.string.app_name)
        }
    }
}

@Composable
private fun conversationSubtitle(
    metadata: ConversationMetadataUiState,
): String? {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> stringResource(id = R.string.loading_messages)

        ConversationMetadataUiState.Unavailable -> null

        is ConversationMetadataUiState.Present -> {
            when {
                shouldShowOneOnOneSubtitle(metadata = metadata) -> {
                    BidiFormatter
                        .getInstance()
                        .unicodeWrap(
                            metadata.otherParticipantDisplayDestination,
                            TextDirectionHeuristicsCompat.LTR,
                        )
                }

                metadata.participantCount > 1 -> {
                    pluralStringResource(
                        id = R.plurals.wearable_participant_count,
                        count = metadata.participantCount,
                        metadata.participantCount,
                    )
                }

                else -> null
            }
        }
    }
}

@Composable
private fun conversationSubtitleContentDescription(
    metadata: ConversationMetadataUiState,
): String? {
    return when (metadata) {
        ConversationMetadataUiState.Loading -> null
        ConversationMetadataUiState.Unavailable -> null
        is ConversationMetadataUiState.Present -> {
            metadata.otherParticipantDisplayDestination
                ?.takeIf {
                    shouldShowOneOnOneSubtitle(metadata = metadata) &&
                        metadata.otherParticipantPhoneNumber != null
                }
                ?.let { displayDestination ->
                    AccessibilityUtil.getVocalizedPhoneNumber(
                        LocalResources.current,
                        displayDestination,
                    )
                }
                ?.takeIf { it.isNotBlank() }
        }
    }
}

private fun shouldShowOneOnOneSubtitle(
    metadata: ConversationMetadataUiState.Present,
): Boolean {
    val displayDestination = metadata.otherParticipantDisplayDestination
        ?.takeIf { it.isNotBlank() }

    return when {
        displayDestination == null -> false
        !metadata.otherParticipantContactLookupKey.isNullOrBlank() -> false
        displayDestination.equals(other = metadata.title, ignoreCase = false) -> false
        else -> true
    }
}

@Immutable
private data class ConversationTopAppBarPresentation(
    val title: String,
    val subtitle: String?,
    val subtitleContentDescription: String?,
    val avatar: ConversationMetadataUiState.Avatar,
    val isBlocked: Boolean,
)

@Immutable
private data class ConversationTopAppBarOverflowVisibility(
    val isAddPeopleVisible: Boolean,
    val isArchiveVisible: Boolean,
    val isUnarchiveVisible: Boolean,
    val isAddContactVisible: Boolean,
    val isDeleteConversationVisible: Boolean,
    val isShowSubjectFieldVisible: Boolean,
    val isSimSelectorVisible: Boolean,
) {
    val isOverflowVisible: Boolean
        get() {
            return isAddPeopleVisible ||
                isArchiveVisible ||
                isUnarchiveVisible ||
                isAddContactVisible ||
                isDeleteConversationVisible ||
                isShowSubjectFieldVisible ||
                isSimSelectorVisible
        }
}

@PreviewLightDark
@Composable
private fun ConversationTopAppBarLoadingPreview() {
    MessagingPreviewTheme {
        ConversationTopAppBar(
            metadata = ConversationMetadataUiState.Loading,
            onAddPeopleClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationTopAppBarOneOnOnePreview() {
    MessagingPreviewTheme {
        ConversationTopAppBar(
            metadata = previewMetadata(),
            isCallVisible = true,
            isArchiveVisible = true,
            isAddContactVisible = true,
            isDeleteConversationVisible = true,
            isShowSubjectFieldVisible = true,
            simSelector = previewSimSelectorUiState(),
            onAddPeopleClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationTopAppBarBlockedPreview() {
    MessagingPreviewTheme {
        ConversationTopAppBar(
            metadata = previewMetadata(isBlocked = true),
            isCallVisible = true,
            isDeleteConversationVisible = true,
            onAddPeopleClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationTopAppBarGroupPreview() {
    MessagingPreviewTheme {
        ConversationTopAppBar(
            metadata = previewGroupMetadata(),
            isAddPeopleVisible = true,
            isUnarchiveVisible = true,
            isDeleteConversationVisible = true,
            onAddPeopleClick = {},
            onTitleClick = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConversationTopAppBarOverflowMenuContentPreview() {
    MessagingPreviewColumn {
        ConversationTopAppBarOverflowMenuContent(
            visibility = ConversationTopAppBarOverflowVisibility(
                isAddPeopleVisible = true,
                isArchiveVisible = true,
                isUnarchiveVisible = true,
                isAddContactVisible = true,
                isDeleteConversationVisible = true,
                isShowSubjectFieldVisible = true,
                isSimSelectorVisible = true,
            ),
            simSelectorLabel = "Personal",
            onAddPeopleClick = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onAddContactClick = {},
            onDeleteConversationClick = {},
            onShowSubjectFieldClick = {},
            onSimSelectorClick = {},
            onItemClick = { action -> action() },
        )
    }
}
