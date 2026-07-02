package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.common.BlockedParticipantItem
import com.android.messaging.ui.blockedparticipants.common.BlockedParticipantsTopAppBar
import com.android.messaging.ui.blockedparticipants.common.ItemDividerHorizontalInset
import com.android.messaging.ui.blockedparticipants.common.ScreenContentPadding
import com.android.messaging.ui.blockedparticipants.common.contentSurfaceShape
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantUiState
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsAction as Action
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsNavEvent as NavEvent
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsUiState as State
import com.android.messaging.ui.common.components.horizontalSafeDrawingInsets
import com.android.messaging.ui.core.MessagingPreviewTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun BlockedParticipantsScreen(
    effectHandler: BlockedParticipantsEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: BlockedParticipantsScreenModel = viewModel<BlockedParticipantsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    val currentEffectHandler by rememberUpdatedState(effectHandler)
    LaunchedEffect(screenModel) {
        screenModel.effects.collect { effect ->
            currentEffectHandler.handle(effect)
        }
    }

    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    LaunchedEffect(screenModel) {
        screenModel.navigationEvents.collect { event ->
            when (event) {
                NavEvent.CloseAfterLastUnblock -> currentOnNavigateBack()
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            BlockedParticipantsTopAppBar(
                onNavigateBack = onNavigateBack,
                selectedCount = uiState.selectedParticipantIds.size,
                onClearSelectionClick = { onAction(Action.ClearSelectionClicked) },
                onDeleteClick = { showDeleteConfirmation = true },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .clip(MaterialTheme.contentSurfaceShape)
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
                        scaffoldContentPadding = contentPadding,
                    )
                }
            }
        }
    }

    BlockedParticipantsDialogs(
        selectedCount = uiState.selectedParticipantIds.size,
        onAction = onAction,
        showDeleteConfirmation = showDeleteConfirmation,
        onDismissDeleteConfirmation = { showDeleteConfirmation = false },
    )
}

@Composable
private fun BlockedParticipantsList(
    uiState: State,
    onAction: (Action) -> Unit,
    scaffoldContentPadding: PaddingValues,
) {
    val layoutDirection = LocalLayoutDirection.current
    val horizontalInsets = horizontalSafeDrawingInsets()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = ScreenContentPadding,
            bottom = ScreenContentPadding + scaffoldContentPadding.calculateBottomPadding(),
            start = ScreenContentPadding + horizontalInsets.calculateStartPadding(layoutDirection),
            end = ScreenContentPadding + horizontalInsets.calculateEndPadding(layoutDirection),
        ),
    ) {
        itemsIndexed(
            items = uiState.participants,
            key = { _, participant -> participant.participantId },
        ) { index, participant ->
            Column {
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = ItemDividerHorizontalInset,
                            vertical = 1.dp,
                        ),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                }

                val destination = participant.normalizedDestination.orEmpty()
                val hasDestination = destination.isNotEmpty()
                val isSelected = participant.participantId in uiState.selectedParticipantIds
                val inSelectionMode = uiState.selectedParticipantIds.isNotEmpty()

                BlockedParticipantItem(
                    participant = participant,
                    isSelected = isSelected,
                    inSelectionMode = inSelectionMode,
                    onClick = {
                        onAction(Action.ParticipantClicked(participant.participantId))
                    },
                    onLongClick = {
                        onAction(Action.ParticipantLongClicked(participant.participantId))
                    },
                    onUnblockClick = {
                        if (hasDestination) {
                            onAction(Action.UnblockClicked(destination))
                        }
                    },
                    onMessageClick = {
                        onAction(Action.ParticipantMessageClicked(participant.conversationId))
                    },
                    onCallClick = {
                        onAction(Action.ParticipantCallClicked(destination))
                    }.takeIf { participant.canCall },
                    onContactClick = {
                        onAction(Action.ParticipantContactInfoClicked(participant))
                    }.takeIf { participant.canShowContact },
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

@PreviewLightDark
@Composable
private fun BlockedParticipantsContentPreview() {
    MessagingPreviewTheme {
        BlockedParticipantsContent(
            uiState = State(
                isLoading = false,
                participants = persistentListOf(
                    BlockedParticipantUiState(
                        participantId = "1",
                        conversationId = "c1",
                        avatarUri = null,
                        displayName = "Spam Caller",
                        details = "+31 6 1234 5678",
                        contactId = 1L,
                        lookupKey = null,
                        normalizedDestination = "+31612345678",
                        canCall = true,
                        canShowContact = true,
                        isContactSaved = true,
                    ),
                    BlockedParticipantUiState(
                        participantId = "2",
                        conversationId = "c2",
                        avatarUri = null,
                        displayName = "+31 6 0000 1111",
                        details = null,
                        contactId = -1L,
                        lookupKey = null,
                        normalizedDestination = "+31600001111",
                        canCall = true,
                        canShowContact = true,
                        isContactSaved = false,
                    ),
                ),
                selectedParticipantIds = persistentSetOf("2"),
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun BlockedParticipantsEmptyPreview() {
    MessagingPreviewTheme {
        BlockedParticipantsContent(
            uiState = State(isLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
