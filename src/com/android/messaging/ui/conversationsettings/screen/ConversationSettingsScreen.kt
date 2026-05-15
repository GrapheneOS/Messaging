package com.android.messaging.ui.conversationsettings.screen

import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.data.conversation.model.notification.SnoozeOption
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.ui.conversation.ConversationActivity
import com.android.messaging.ui.conversationsettings.common.ConversationHeader
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsItem
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsTopAppBar
import com.android.messaging.ui.conversationsettings.common.ConversationSimAvatar
import com.android.messaging.ui.conversationsettings.common.ParticipantItem
import com.android.messaging.ui.conversationsettings.common.resolveDisplayName
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsNavRouteSavedState as NavRouteSavedState
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavEvent as NavEvent
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavRoute as NavRoute
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State

private const val SLIDE_OFFSET_DIVISOR = 3

@Composable
internal fun ConversationSettingsScreen(
    effectHandler: ConversationSettingsEffectHandler,
    onNavigateBack: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: ConversationSettingsScreenModel = viewModel<ConversationSettingsViewModel>(),
) {
    val uiState by screenModel.uiState.collectAsStateWithLifecycle()
    val rootConversationId = screenModel.rootConversationId

    var currentRoute by rememberSaveable(
        stateSaver = NavRouteSavedState.Saver,
    ) {
        mutableStateOf(NavRoute.Conversation)
    }
    val targetConversationId = currentRoute.targetConversationId(rootConversationId)

    fun isRootRoute() = currentRoute is NavRoute.Conversation

    LaunchedEffect(targetConversationId) {
        screenModel.setConversationId(targetConversationId)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        screenModel.refreshState()
    }

    LaunchedEffect(screenModel, effectHandler) {
        screenModel.effects.collect(effectHandler::handle)
    }

    var resultCode by remember { mutableStateOf<Int?>(null) }
    val navigateUp: () -> Unit = {
        if (isRootRoute()) {
            onNavigateBack(resultCode)
        } else {
            currentRoute = NavRoute.Conversation
        }
    }

    LaunchedEffect(screenModel) {
        screenModel.navigationEvents.collect { event ->
            when (event) {
                is NavEvent.OpenParticipantInfo -> {
                    currentRoute = NavRoute.ParticipantInfo(
                        conversationId = event.conversationId,
                    )
                }

                NavEvent.CloseAfterBlock,
                NavEvent.CloseAfterArchive,
                    -> {
                    if (isRootRoute()) {
                        resultCode = ConversationActivity.FINISH_RESULT_CODE
                    }
                    navigateUp()
                }
            }
        }
    }

    BackHandler(
        enabled = !isRootRoute(),
        onBack = navigateUp,
    )

    ConversationSettingsNavHost(
        route = currentRoute,
        rootConversationId = rootConversationId,
        uiState = uiState,
        onAction = screenModel::onAction,
        onNavigateBack = navigateUp,
        modifier = modifier,
    )
}

@Composable
private fun ConversationSettingsNavHost(
    route: NavRoute,
    rootConversationId: String,
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = route,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        transitionSpec = {
            val isForward = targetState.depth > initialState.depth
            if (isForward) {
                (slideInHorizontally { it / SLIDE_OFFSET_DIVISOR } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / SLIDE_OFFSET_DIVISOR } + fadeOut())
            } else {
                (slideInHorizontally { -it / SLIDE_OFFSET_DIVISOR } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / SLIDE_OFFSET_DIVISOR } + fadeOut())
            }
        },
        label = "conversation_settings_navigation",
    ) { animatedRoute ->
        val displayed = rememberDisplayedConversation(
            targetConversationId = animatedRoute.targetConversationId(rootConversationId),
            uiState = uiState,
        )

        ConversationSettingsContent(
            uiState = displayed,
            onAction = onAction,
            onNavigateBack = onNavigateBack,
        )
    }
}

private fun NavRoute.targetConversationId(
    rootConversationId: String,
): String {
    return when (this) {
        NavRoute.Conversation -> rootConversationId
        is NavRoute.ParticipantInfo -> conversationId
    }
}

@Composable
private fun rememberDisplayedConversation(
    targetConversationId: String,
    uiState: State,
): State {
    val current = uiState.takeIf { it.conversationId == targetConversationId }
    var cached by remember(targetConversationId) { mutableStateOf(current) }
    SideEffect {
        if (current != null && cached != current) {
            cached = current
        }
    }
    return current ?: cached ?: uiState
}

@Composable
private fun ConversationSettingsContent(
    uiState: State,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingBlockConfirmation by remember { mutableStateOf(false) }
    var showSnoozeChatDialog by remember { mutableStateOf(false) }

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

            contactItems(
                uiState = uiState,
                onAction = onAction,
            )

            simSwitchItem(
                uiState = uiState,
                onAction = onAction,
            )

            generalSettingsItems(
                uiState = uiState,
                onAction = onAction,
                onRequestBlockConfirmation = { pendingBlockConfirmation = true },
                onRequestSnoozeChooser = { showSnoozeChatDialog = true },
            )

            participantsItems(
                uiState = uiState,
                onAction = onAction,
            )
        }
    }

    ConversationSettingsDialogs(
        uiState = uiState,
        onAction = onAction,
        pendingBlockConfirmation = pendingBlockConfirmation,
        showSnoozeChatDialog = showSnoozeChatDialog,
        onDismissBlockConfirmation = { pendingBlockConfirmation = false },
        onDismissSnoozeChat = { showSnoozeChatDialog = false },
    )
}

private fun LazyListScope.contactItems(
    uiState: State,
    onAction: (Action) -> Unit,
) {
    if (uiState.otherParticipant == null || uiState.participants.size > 1) return
    if (!uiState.canCall && !uiState.canShowContact) return

    item(key = "contact_buttons") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.canCall) {
                ContactButtonItem(
                    imageVector = Icons.Default.Call,
                    text = stringResource(R.string.action_call),
                    onClick = { onAction(Action.CallClicked) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (uiState.canShowContact) {
                val (icon, textRes) = if (uiState.isContactSaved) {
                    Icons.Default.Person to R.string.action_contact_info
                } else {
                    Icons.Default.PersonAdd to R.string.action_add_contact
                }
                ContactButtonItem(
                    imageVector = icon,
                    text = stringResource(textRes),
                    onClick = { onAction(Action.ContactInfoClicked) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ContactButtonItem(
    imageVector: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceContainer,
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun LazyListScope.generalSettingsItems(
    uiState: State,
    onAction: (Action) -> Unit,
    onRequestBlockConfirmation: () -> Unit,
    onRequestSnoozeChooser: () -> Unit,
) {
    item(key = "snooze") {
        val titleRes = if (uiState.isSnoozed) {
            R.string.unsnooze_chat_setting_title
        } else {
            R.string.snooze_chat_setting_title
        }

        ConversationSettingsItem(
            icon = Icons.Default.Snooze,
            title = stringResource(titleRes),
            onClick = {
                if (uiState.isSnoozed) {
                    onAction(Action.UnsnoozeClicked)
                } else {
                    onRequestSnoozeChooser()
                }
            },
        )
    }

    item(key = "notifications") {
        ConversationSettingsItem(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.notifications_enabled_conversation_pref_title),
            onClick = { onAction(Action.NotificationsClicked) },
        )
    }

    item(key = "archive") {
        val (icon, titleRes) = if (uiState.isArchived) {
            Icons.Default.Unarchive to R.string.action_unarchive
        } else {
            Icons.Default.Archive to R.string.action_archive
        }

        ConversationSettingsItem(
            icon = icon,
            title = stringResource(titleRes),
            onClick = {
                if (uiState.isArchived) {
                    onAction(Action.UnarchiveClicked)
                } else {
                    onAction(Action.ArchiveClicked)
                }
            },
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

private fun LazyListScope.simSwitchItem(
    uiState: State,
    onAction: (Action) -> Unit,
) {
    if (!uiState.isSimSwitchAvailable) return

    val selected = uiState.selectedSubscription ?: return

    item(key = "sim_switch") {
        SimSwitchItem(
            subscriptions = uiState.availableSubscriptions,
            selected = selected,
            onSimSelected = { selfParticipantId ->
                onAction(Action.SimSelected(selfParticipantId))
            },
        )
    }
}

@Composable
private fun SimSwitchItem(
    subscriptions: ImmutableList<Subscription>,
    selected: Subscription,
    onSimSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = { expanded = true },
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.SimCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sim_selector_item_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val subtitle = selected.displayDestination
                    ?: selected.label.resolveDisplayName()
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = stringResource(
                            R.string.sim_selector_item_title,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    shape = RoundedCornerShape(20.dp),
                    onDismissRequest = { expanded = false },
                ) {
                    SimSelectorPopupContent(
                        subscriptions = subscriptions,
                        onSimSelected = { id ->
                            expanded = false
                            onSimSelected(id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SimSelectorPopupContent(
    subscriptions: ImmutableList<Subscription>,
    onSimSelected: (String) -> Unit,
) {
    Column {
        subscriptions.forEach { subscription ->
            SimSelectorRow(
                subscription = subscription,
                onClick = { onSimSelected(subscription.selfParticipantId) },
            )
        }
    }
}

@Composable
private fun SimSelectorRow(
    subscription: Subscription,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        ConversationSimAvatar(subscription = subscription)

        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = subscription.label.resolveDisplayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            subscription.displayDestination?.let { destination ->
                Text(
                    text = destination,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun LazyListScope.participantsItems(
    uiState: State,
    onAction: (Action) -> Unit,
) {
    val participants = uiState.participants
    if (participants.isEmpty()) return

    val showParticipantActions = uiState.otherParticipant == null && participants.size > 1

    item(key = "participants_group") {
        ParticipantsCard(
            participants = participants,
            showParticipantActions = showParticipantActions,
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
    showParticipantActions: Boolean,
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
                val hasInfoAction =
                    showParticipantActions && participant.normalizedDestination != null

                ParticipantItem(
                    participant = participant,
                    onClick = { onParticipantClick(participant) },
                    onLongClick = { onParticipantLongClick(participant) },
                    onAction = { onParticipantActionClick(participant) }
                        .takeIf { hasInfoAction },
                )
            }
        }
    }
}

@Composable
private fun ConversationSettingsDialogs(
    uiState: State,
    onAction: (Action) -> Unit,
    pendingBlockConfirmation: Boolean,
    showSnoozeChatDialog: Boolean,
    onDismissBlockConfirmation: () -> Unit,
    onDismissSnoozeChat: () -> Unit,
) {
    if (pendingBlockConfirmation) {
        val displayName = uiState.otherParticipant?.displayDestination.orEmpty()

        BlockConfirmationDialog(
            displayName = displayName,
            onDismiss = onDismissBlockConfirmation,
            onConfirm = {
                onAction(Action.BlockConfirmed)
                onDismissBlockConfirmation()
            },
        )
    }

    if (showSnoozeChatDialog) {
        SnoozeChatDialog(
            onDismiss = onDismissSnoozeChat,
            onConfirm = { option ->
                onAction(Action.SnoozeOptionSelected(option))
                onDismissSnoozeChat()
            },
        )
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

@Composable
private fun SnoozeChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (SnoozeOption) -> Unit,
) {
    var selectedOption by rememberSaveable { mutableStateOf(SnoozeOption.OneHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Snooze,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.snooze_chat_dialog_title),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                Text(
                    text = stringResource(R.string.snooze_chat_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                SnoozeOption.entries.forEach { option ->
                    SnoozeOptionRow(
                        text = stringResource(option.labelRes),
                        selected = option == selectedOption,
                        onClick = { selectedOption = option },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedOption) }) {
                Text(text = stringResource(R.string.snooze_chat_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

private val SnoozeOption.labelRes: Int
    get() = when (this) {
        SnoozeOption.OneHour -> R.string.snooze_chat_option_one_hour
        SnoozeOption.EightHours -> R.string.snooze_chat_option_eight_hours
        SnoozeOption.TwentyFourHours -> R.string.snooze_chat_option_twenty_four_hours
        SnoozeOption.Always -> R.string.snooze_chat_option_always
    }

@Composable
private fun SnoozeOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Preview
@Composable
private fun SnoozeChatDialogPreview() {
    AppTheme {
        SnoozeChatDialog(
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun ConversationSettingsContentPreview() {
    AppTheme {
        ConversationSettingsContent(
            uiState = State(
                conversationId = "1",
                conversationTitle = "Family",
                participants = persistentListOf(
                    ParticipantUiState(
                        participantId = "+31612345678",
                        avatarUri = null,
                        displayName = "Mother",
                        details = "+31 6 1234 5678",
                        contactId = 1L,
                        lookupKey = null,
                        normalizedDestination = "+31612345678",
                        isBlocked = false,
                        displayDestination = "+31 6 1234 5678",
                    ),
                    ParticipantUiState(
                        participantId = "+31687654321",
                        avatarUri = null,
                        displayName = "Father",
                        details = "+31 6 8765 4321",
                        contactId = 2L,
                        lookupKey = null,
                        normalizedDestination = "+31687654321",
                        isBlocked = false,
                        displayDestination = "+31 6 8765 4321",
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
private fun BlockConfirmationDialogPreview() {
    AppTheme {
        BlockConfirmationDialog(
            displayName = "+31 6 1234 5678",
            onDismiss = {},
            onConfirm = {},
        )
    }
}
