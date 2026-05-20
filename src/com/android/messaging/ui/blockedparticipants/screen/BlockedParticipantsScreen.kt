package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.common.ContentSurfaceShape
import com.android.messaging.ui.blockedparticipants.common.ConversationSettingsTopAppBar
import com.android.messaging.ui.blockedparticipants.common.ItemDividerHorizontalInset
import com.android.messaging.ui.blockedparticipants.common.ItemHorizontalPadding
import com.android.messaging.ui.blockedparticipants.common.ItemShape
import com.android.messaging.ui.blockedparticipants.common.ItemVerticalPadding
import com.android.messaging.ui.blockedparticipants.common.ScreenContentPadding
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction as Action
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsNavEvent as NavEvent
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState as State
import com.android.messaging.ui.common.components.ParticipantAvatar
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun BlockedParticipantsScreen(
    effectHandler: BlockedParticipantsEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: BlockedParticipantsScreenModel = viewModel<BlockedParticipantsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(screenModel, effectHandler) {
        screenModel.effects.collect(effectHandler::handle)
    }

    LaunchedEffect(screenModel, onNavigateBack) {
        screenModel.navigationEvents.collect { event ->
            when (event) {
                NavEvent.CloseAfterLastUnblock -> onNavigateBack()
            }
        }
    }

    BlockedParticipantsContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedParticipantsContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = { ConversationSettingsTopAppBar(onNavigateBack = onNavigateBack) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .clip(ContentSurfaceShape)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                uiState.isLoading -> Unit

                uiState.participants.isEmpty() -> {
                    BlockedParticipantsEmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = contentPadding.calculateBottomPadding()),
                    )
                }

                else -> {
                    BlockedParticipantsList(
                        uiState = uiState,
                        onAction = onAction,
                        bottomPadding = contentPadding.calculateBottomPadding(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedParticipantsList(
    uiState: State,
    onAction: (Action) -> Unit,
    bottomPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = ScreenContentPadding,
            bottom = ScreenContentPadding + bottomPadding,
            start = ScreenContentPadding,
            end = ScreenContentPadding,
        ),
    ) {
        itemsIndexed(
            items = uiState.participants,
            key = { _, participant -> participant.participantId },
        ) { index, participant ->
            Column {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = ItemDividerHorizontalInset),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                }

                BlockedParticipantItem(
                    participant = participant,
                    isSelected = false,
                    onUnblockClick = {
                        val destination = participant.normalizedDestination
                            ?: return@BlockedParticipantItem

                        onAction(Action.UnblockClicked(destination))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlockedParticipantItem(
    participant: BlockedParticipantUiState,
    isSelected: Boolean,
    onUnblockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.background
    }

    val details = participant.details

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ItemShape,
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = {},
                )
                .padding(
                    horizontal = ItemHorizontalPadding,
                    vertical = ItemVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ParticipantAvatar(
                avatarUri = participant.avatarUri,
                size = 48.dp,
                fallbackIcon = Icons.Default.Person,
            )

            Spacer(modifier = Modifier.width(ItemHorizontalPadding))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = ItemHorizontalPadding),
            ) {
                Text(
                    text = participant.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = details.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onUnblockClick) {
                Icon(
                    imageVector = Icons.Outlined.PersonRemove,
                    contentDescription = stringResource(R.string.tap_to_unblock_message),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BlockedParticipantsEmptyState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(ScreenContentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = stringResource(R.string.blocked_contacts_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun BlockedParticipantsContentPreview() {
    AppTheme {
        BlockedParticipantsContent(
            uiState = State(
                isLoading = false,
                participants = persistentListOf(
                    BlockedParticipantUiState(
                        participantId = "1",
                        avatarUri = null,
                        displayName = "Spam Caller",
                        details = "+31 6 1234 5678",
                        contactId = 1L,
                        lookupKey = null,
                        normalizedDestination = "+31612345678",
                    ),
                    BlockedParticipantUiState(
                        participantId = "2",
                        avatarUri = null,
                        displayName = "+31 6 0000 1111",
                        details = null,
                        contactId = -1L,
                        lookupKey = null,
                        normalizedDestination = "+31600001111",
                    ),
                ),
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun BlockedParticipantsEmptyPreview() {
    AppTheme {
        BlockedParticipantsContent(
            uiState = State(isLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
