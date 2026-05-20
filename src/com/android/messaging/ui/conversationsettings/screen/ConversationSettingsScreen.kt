package com.android.messaging.ui.conversationsettings.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State as ComposeState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.messaging.R
import com.android.messaging.ui.conversation.ConversationActivity
import com.android.messaging.ui.conversationsettings.common.ConversationHeader
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsItem
import com.android.messaging.ui.conversationsettings.common.ConversationSettingsTopAppBar
import com.android.messaging.ui.conversationsettings.common.GroupedBottomItemShape
import com.android.messaging.ui.conversationsettings.common.GroupedItemSpacing
import com.android.messaging.ui.conversationsettings.common.GroupedMiddleItemShape
import com.android.messaging.ui.conversationsettings.common.GroupedTopItemShape
import com.android.messaging.ui.conversationsettings.common.ParticipantItem
import com.android.messaging.ui.conversationsettings.common.ScreenContentPadding
import com.android.messaging.ui.conversationsettings.common.SectionSpacing
import com.android.messaging.ui.conversationsettings.common.SettingsCardShape
import com.android.messaging.ui.conversationsettings.screen.ConversationSettingsNavRouteSavedState as NavRouteSavedState
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsAction as Action
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavEvent as NavEvent
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavRoute as NavRoute
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsUiState as State
import com.android.messaging.ui.conversationsettings.screen.model.ParticipantUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

                NavEvent.CloseAfterArchive -> {
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

        if (displayed != null) {
            ConversationSettingsContent(
                uiState = displayed,
                onAction = onAction,
                onNavigateBack = onNavigateBack,
            )
        } else {
            ConversationSettingsPlaceholder(
                onNavigateBack = onNavigateBack,
            )
        }
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
): State? {
    val current = uiState.takeIf { it.conversationId == targetConversationId }
    var cached by remember(targetConversationId) { mutableStateOf(current) }
    SideEffect {
        if (current != null && cached != current) {
            cached = current
        }
    }
    return current ?: cached
}

@Composable
private fun ConversationSettingsPlaceholder(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ConversationSettingsTopAppBar(
            title = "",
            participant = null,
            onNavigateBack = onNavigateBack,
            collapseProgress = { 0f },
        )
    }
}

@Composable
private fun rememberCollapseProgress(
    listState: LazyListState,
): ComposeState<Float> {
    return remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                return@derivedStateOf 1f
            }

            val headerInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (headerInfo == null || headerInfo.size == 0) {
                return@derivedStateOf 0f
            }

            val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
            (scrollOffset / headerInfo.size.toFloat()).coerceIn(0f, 1f)
        }
    }
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

    val listState = rememberLazyListState()
    val collapseProgress = rememberCollapseProgress(listState)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ConversationSettingsTopAppBar(
                title = uiState.conversationTitle,
                participant = uiState.otherParticipant,
                onNavigateBack = onNavigateBack,
                collapseProgress = { collapseProgress.value },
            )
        },
    ) { contentPadding ->
        ConversationSettingsList(
            uiState = uiState,
            onAction = onAction,
            listState = listState,
            collapseProgress = { collapseProgress.value },
            contentPadding = contentPadding,
            onRequestBlockConfirmation = { pendingBlockConfirmation = true },
            onRequestSnoozeChooser = { showSnoozeChatDialog = true },
        )
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

@Composable
private fun ConversationSettingsList(
    uiState: State,
    onAction: (Action) -> Unit,
    listState: LazyListState,
    collapseProgress: () -> Float,
    contentPadding: PaddingValues,
    onRequestBlockConfirmation: () -> Unit,
    onRequestSnoozeChooser: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + ScreenContentPadding,
            start = ScreenContentPadding,
            end = ScreenContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        item(key = "header") {
            ConversationHeader(
                title = uiState.conversationTitle,
                participant = uiState.otherParticipant,
                collapseProgress = collapseProgress,
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
            onRequestBlockConfirmation = onRequestBlockConfirmation,
            onRequestSnoozeChooser = onRequestSnoozeChooser,
        )

        participantsItems(
            uiState = uiState,
            onAction = onAction,
        )
    }
}

private fun LazyListScope.contactItems(
    uiState: State,
    onAction: (Action) -> Unit,
) {
    if (uiState.otherParticipant == null) return
    if (!uiState.canCall && !uiState.canShowContact) return

    item(key = "contact_buttons") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SectionSpacing),
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
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = text,
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
    item(key = "general_settings") {
        GeneralSettingsCard(
            uiState = uiState,
            onAction = onAction,
            onRequestSnoozeChooser = onRequestSnoozeChooser,
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

@Composable
private fun GeneralSettingsCard(
    uiState: State,
    onAction: (Action) -> Unit,
    onRequestSnoozeChooser: () -> Unit,
) {
    val snoozeTitleRes = if (uiState.isSnoozed) {
        R.string.unsnooze_chat_setting_title
    } else {
        R.string.snooze_chat_setting_title
    }

    val (archiveIcon, archiveTitleRes) = if (uiState.isArchived) {
        Icons.Default.Unarchive to R.string.action_unarchive
    } else {
        Icons.Default.Archive to R.string.action_archive
    }

    Column(verticalArrangement = Arrangement.spacedBy(GroupedItemSpacing)) {
        ConversationSettingsItem(
            icon = Icons.Default.Snooze,
            title = stringResource(snoozeTitleRes),
            onClick = {
                if (uiState.isSnoozed) {
                    onAction(Action.UnsnoozeClicked)
                } else {
                    onRequestSnoozeChooser()
                }
            },
            shape = GroupedTopItemShape,
        )

        ConversationSettingsItem(
            icon = Icons.Default.Notifications,
            title = stringResource(R.string.notifications_enabled_conversation_pref_title),
            onClick = { onAction(Action.NotificationsClicked) },
            shape = GroupedMiddleItemShape,
        )

        ConversationSettingsItem(
            icon = archiveIcon,
            title = stringResource(archiveTitleRes),
            onClick = {
                if (uiState.isArchived) {
                    onAction(Action.UnarchiveClicked)
                } else {
                    onAction(Action.ArchiveClicked)
                }
            },
            shape = GroupedBottomItemShape,
        )
    }
}

private fun LazyListScope.participantsItems(
    uiState: State,
    onAction: (Action) -> Unit,
) {
    val participants = uiState.participants
    if (participants.isEmpty()) return

    val showParticipantActions = participants.size > 1

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
        shape = SettingsCardShape,
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
private fun ContactButtonItemPreview() {
    AppTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(SectionSpacing)) {
            ContactButtonItem(
                imageVector = Icons.Default.Call,
                text = "Call",
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            ContactButtonItem(
                imageVector = Icons.Default.Person,
                text = "Contact info",
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}
