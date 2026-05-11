package com.android.messaging.ui.conversationsettings.screen

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import kotlinx.collections.immutable.ImmutableList
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action

// TODO: Update contentDescriptions and constants and think about the design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationSettingsScreen(
    effectHandler: ConversationSettingsEffectHandler,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ConversationSettingsScreenModel = viewModel<ConversationSettingsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()
    var pendingBlockConfirmation by remember { mutableStateOf(false) }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        screenModel.refreshState()
    }

    LaunchedEffect(screenModel, effectHandler) {
        screenModel.effects.collect(effectHandler::handle)
    }

    // TODO: Extract as a separate component
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
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
                ConversationHeader(title = uiState.conversationTitle)
            }

            generalSettingsItems(
                uiState = uiState,
                onAction = screenModel::onAction,
                onRequestBlockConfirmation = { pendingBlockConfirmation = true },
            )

            participantsItems(
                participants = uiState.participants,
                onAction = screenModel::onAction,
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
                screenModel.onAction(Action.BlockConfirmed)
            },
        )
    }
}

@Composable
private fun ConversationHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            // TODO: Add a user avatar
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        if (title.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun LazyListScope.generalSettingsItems(
    uiState: ConversationSettingsUiState,
    onAction: (Action) -> Unit,
    onRequestBlockConfirmation: () -> Unit,
) {
    item(key = "notifications") {
        SettingsCard(
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
            SettingsCard(
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
            onParticipantLongClick = { participant ->
                participant.details?.takeIf { it.isNotEmpty() }?.let {
                    onAction(Action.ParticipantLongPressed(it))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 18.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun ParticipantsCard(
    participants: ImmutableList<ParticipantUiState>,
    onParticipantLongClick: (ParticipantUiState) -> Unit,
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
                ParticipantRow(
                    participant = participant,
                    onLongClick = { onParticipantLongClick(participant) },
                )
            }
        }
    }
}


// TODO: Extract as a separate component.
//  It seems that this might be common for the whole project
@Composable
private fun ParticipantRow(
    participant: ParticipantUiState,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { TODO("Implement a dialogue with a participant") },
                onLongClick = onLongClick,
            )
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParticipantAvatar(avatarUri = participant.avatarUri)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = participant.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!participant.details.isNullOrEmpty()) {
                Text(
                    text = participant.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(
    avatarUri: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            avatarUri.isNullOrBlank() -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            else -> {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

// TODO: Extract as a separate component
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

// TODO: Add previews
