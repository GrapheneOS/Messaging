package com.android.messaging.ui.conversationsettings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.common.ConversationHeader
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsItem
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsTopAppBar
import com.android.messaging.ui.conversationsettings.common.ParticipantItem
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action

@Composable
internal fun ConversationSettingsScreen(
    effectHandler: ConversationSettingsEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ConversationSettingsScreenModel = viewModel<ConversationSettingsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(screenModel, effectHandler) {
        screenModel.effects.collect(effectHandler::handle)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        screenModel.refreshState()
    }

    ConversationSettingsContent(
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationSettingsContent(
    uiState: ConversationSettingsUiState,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingBlockConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ConversationSettingsTopAppBar(onNavigateBack = onNavigateBack)
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ConversationHeader(
                    title = uiState.conversationTitle,
                    participant = uiState.otherParticipant,
                )
            }

            generalSettingsItems(
                uiState = uiState,
                onAction = onAction,
                onRequestBlockConfirmation = { pendingBlockConfirmation = true },
            )

            participantsItems(
                participants = uiState.participants,
                onAction = onAction,
            )
        }
    }

    if (pendingBlockConfirmation) {
        val displayName = uiState.otherParticipant?.displayDestination.orEmpty()

        BlockConfirmationDialog(
            displayName = displayName,
            onDismiss = { pendingBlockConfirmation = false },
            onConfirm = {
                pendingBlockConfirmation = false
                onAction(Action.BlockConfirmed)
            },
        )
    }
}

private fun LazyListScope.generalSettingsItems(
    uiState: ConversationSettingsUiState,
    onAction: (Action) -> Unit,
    onRequestBlockConfirmation: () -> Unit,
) {
    item(key = "notifications") {
        ConversationSettingsItem(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.notifications_enabled_conversation_pref_title),
            onClick = { onAction(Action.NotificationsClicked) },
        )
    }

    val otherParticipant = uiState.otherParticipant
    if (otherParticipant != null) {
        val titleRes = if (otherParticipant.isBlocked) {
            R.string.unblock_contact_title
        } else {
            R.string.block_contact_title
        }

        item(key = "block") {
            ConversationSettingsItem(
                icon = Icons.Default.Block,
                title = stringResource(titleRes, otherParticipant.displayDestination.orEmpty()),
                onClick = {
                    if (otherParticipant.isBlocked) {
                        onAction(Action.UnblockClicked)
                    } else {
                        onRequestBlockConfirmation()
                    }
                },
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun LazyListScope.participantsItems(
    participants: ImmutableList<ParticipantUiState>,
    onAction: (Action) -> Unit,
) {
    if (participants.isEmpty()) return

    item(key = "participants_group") {
        ParticipantsCard(
            participants = participants,
            onParticipantClick = { participant ->
                val destination = participant.normalizedDestination ?: return@ParticipantsCard
                onAction(Action.ParticipantPressed(destination))
            },
            onParticipantLongClick = { participant ->
                participant.details?.takeIf { it.isNotEmpty() }?.let {
                    onAction(Action.ParticipantLongPressed(it))
                }
            },
            onParticipantActionClick = { participant ->
                val destination = participant.normalizedDestination ?: return@ParticipantsCard
                onAction(Action.ParticipantActionPressed(destination))
            },
        )
    }
}

@Composable
private fun ParticipantsCard(
    participants: ImmutableList<ParticipantUiState>,
    onParticipantClick: (ParticipantUiState) -> Unit,
    onParticipantLongClick: (ParticipantUiState) -> Unit,
    onParticipantActionClick: (ParticipantUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.participant_list_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            )
            participants.forEach { participant ->
                ParticipantItem(
                    participant = participant,
                    onClick = { onParticipantClick(participant) },
                    onLongClick = { onParticipantLongClick(participant) },
                    onAction = { onParticipantActionClick(participant) },
                )
            }
        }
    }
}

@Composable
private fun BlockConfirmationDialog(
    displayName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.block_confirmation_title, displayName))
        },
        text = {
            Text(text = stringResource(R.string.block_confirmation_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun BlockConfirmationDialogPreview() {
    AppTheme {
        BlockConfirmationDialog(
            displayName = "+31 6 1234 5678",
            onDismiss = {},
            onConfirm = {},
        )
    }
}
