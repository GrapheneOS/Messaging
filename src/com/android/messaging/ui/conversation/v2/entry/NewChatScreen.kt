@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.v2.entry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.ui.conversation.v2.NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.v2.NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.v2.newChatContactRowTestTag
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerModel
import com.android.messaging.ui.conversation.v2.recipientpicker.RecipientPickerViewModel
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val CONTACT_CORNER_RADIUS = 18.dp
private val CONTACT_MIDDLE_CORNER_RADIUS = 2.dp

private val SearchFieldShape = RoundedCornerShape(size = 22.dp)

private val TopContactShape = RoundedCornerShape(
    topStart = CONTACT_CORNER_RADIUS,
    topEnd = CONTACT_CORNER_RADIUS,
    bottomStart = CONTACT_MIDDLE_CORNER_RADIUS,
    bottomEnd = CONTACT_MIDDLE_CORNER_RADIUS,
)
private val BottomContactShape = RoundedCornerShape(
    topStart = CONTACT_MIDDLE_CORNER_RADIUS,
    topEnd = CONTACT_MIDDLE_CORNER_RADIUS,
    bottomStart = CONTACT_CORNER_RADIUS,
    bottomEnd = CONTACT_CORNER_RADIUS,
)
private val MiddleContactShape = RoundedCornerShape(size = CONTACT_MIDDLE_CORNER_RADIUS)
private val SingleContactShape = RoundedCornerShape(size = CONTACT_CORNER_RADIUS)

private const val CONTACTS_LOAD_MORE_THRESHOLD = 10
private const val NEW_CHAT_CONTACT_CONTENT_TYPE = "new_chat_contact"

@Composable
internal fun NewChatScreen(
    modifier: Modifier = Modifier,
    isCreatingGroup: Boolean = false,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    onContactClick: (String) -> Unit = {},
    onContactLongClick: (String) -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onCreateGroupConfirmed: () -> Unit = {},
    onCreateGroupRecipientClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    pickerModel: RecipientPickerModel = hiltViewModel<RecipientPickerViewModel>(),
    resolvingRecipientDestination: String? = null,
    selectedGroupRecipientDestinations: ImmutableList<String> = persistentListOf(),
) {
    val uiState by pickerModel.uiState.collectAsStateWithLifecycle()
    val screenContainerColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        modifier = modifier,
        containerColor = screenContainerColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenContainerColor,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
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
                title = {
                    Text(text = newChatTitle(isCreatingGroup = isCreatingGroup))
                },
            )
        },
    ) { contentPadding ->
        NewChatScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = contentPadding),
            isCreatingGroup = isCreatingGroup,
            uiState = uiState,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            onCreateGroupClick = onCreateGroupClick,
            onCreateGroupConfirmed = onCreateGroupConfirmed,
            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
            onLoadMore = pickerModel::onLoadMore,
            onQueryChanged = pickerModel::onQueryChanged,
            resolvingRecipientDestination = resolvingRecipientDestination,
            selectedGroupRecipientDestinations = selectedGroupRecipientDestinations,
        )
    }
}

@Composable
private fun NewChatScreenContent(
    uiState: RecipientPickerUiState,
    modifier: Modifier = Modifier,
    isCreatingGroup: Boolean = false,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    onContactClick: (String) -> Unit,
    onContactLongClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreateGroupConfirmed: () -> Unit,
    onCreateGroupRecipientClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onQueryChanged: (String) -> Unit,
    resolvingRecipientDestination: String? = null,
    selectedGroupRecipientDestinations: ImmutableList<String> = persistentListOf(),
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        NewChatScreenBody(
            uiState = uiState,
            isCreatingGroup = isCreatingGroup,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            onCreateGroupClick = onCreateGroupClick,
            onCreateGroupConfirmed = onCreateGroupConfirmed,
            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
            onLoadMore = onLoadMore,
            onQueryChanged = onQueryChanged,
            resolvingRecipientDestination = resolvingRecipientDestination,
            selectedGroupRecipientDestinations = selectedGroupRecipientDestinations,
        )
    }
}

@Composable
private fun NewChatScreenBody(
    uiState: RecipientPickerUiState,
    isCreatingGroup: Boolean,
    isResolvingConversation: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    onContactClick: (String) -> Unit,
    onContactLongClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreateGroupConfirmed: () -> Unit,
    onCreateGroupRecipientClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onQueryChanged: (String) -> Unit,
    resolvingRecipientDestination: String?,
    selectedGroupRecipientDestinations: ImmutableList<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(height = 16.dp))

        NewChatQueryField(
            query = uiState.query,
            enabled = !isResolvingConversation,
            onQueryChanged = onQueryChanged,
        )

        Spacer(modifier = Modifier.height(height = 12.dp))

        NewChatContactsContent(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            isCreatingGroup = isCreatingGroup,
            contactSelectionEnabled = !isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = onContactClick,
            onContactLongClick = onContactLongClick,
            onCreateGroupClick = onCreateGroupClick,
            onCreateGroupConfirmed = onCreateGroupConfirmed,
            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
            onLoadMore = onLoadMore,
            resolvingRecipientDestination = resolvingRecipientDestination,
            selectedGroupRecipientDestinations = selectedGroupRecipientDestinations,
        )
    }
}

@Composable
private fun NewChatQueryField(
    query: String,
    enabled: Boolean,
    onQueryChanged: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    TextField(
        modifier = Modifier
            .fillMaxWidth(),
        value = query,
        onValueChange = onQueryChanged,
        enabled = enabled,
        singleLine = true,
        shape = SearchFieldShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colorScheme.surface,
            unfocusedContainerColor = colorScheme.surface,
            disabledContainerColor = colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = colorScheme.onSurface,
            unfocusedTextColor = colorScheme.onSurface,
            disabledTextColor = colorScheme.onSurface,
            focusedPlaceholderColor = colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = colorScheme.onSurfaceVariant,
            focusedPrefixColor = colorScheme.onSurfaceVariant,
            unfocusedPrefixColor = colorScheme.onSurfaceVariant,
            disabledPrefixColor = colorScheme.onSurfaceVariant,
        ),
        prefix = {
            Text(
                modifier = Modifier
                    .padding(end = 12.dp),
                text = stringResource(id = R.string.new_chat_recipient_prefix),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        placeholder = {
            Text(
                text = stringResource(id = R.string.new_chat_query_hint),
            )
        },
    )
}

@Composable
private fun newChatTitle(
    isCreatingGroup: Boolean,
): String {
    return when {
        isCreatingGroup -> stringResource(id = R.string.conversation_new_group)
        else ->  stringResource(id = R.string.start_new_conversation)
    }
}

@Composable
private fun NewChatContactsContent(
    modifier: Modifier = Modifier,
    uiState: RecipientPickerUiState,
    isCreatingGroup: Boolean,
    contactSelectionEnabled: Boolean,
    isResolvingConversationIndicatorVisible: Boolean,
    onContactClick: (String) -> Unit,
    onContactLongClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onCreateGroupConfirmed: () -> Unit,
    onCreateGroupRecipientClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    resolvingRecipientDestination: String?,
    selectedGroupRecipientDestinations: ImmutableList<String>,
) {
    val contacts = uiState.contacts
    val lastContactIndex = contacts.lastIndex
    val listState = rememberLazyListState()
    val showCreateGroupNextButton = isCreatingGroup &&
        selectedGroupRecipientDestinations.isNotEmpty()

    val animatedListBottomPadding by animateDpAsState(
        targetValue = when {
            showCreateGroupNextButton -> 100.dp
            else -> 16.dp
        },
        animationSpec = defaultSpatialAnimationSpec(),
        label = "newChatListBottomPadding",
    )

    LaunchedEffect(
        listState,
        uiState.canLoadMore,
        uiState.isLoading,
        uiState.isLoadingMore,
        contacts.size,
    ) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= lastContactIndex - CONTACTS_LOAD_MORE_THRESHOLD
        }.collect { shouldLoadMore ->
            val isLoading = uiState.isLoading || uiState.isLoadingMore
            if (shouldLoadMore && uiState.canLoadMore && !isLoading) {
                onLoadMore()
            }
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                bottom = animatedListBottomPadding,
            ),
        ) {
            item {
                AnimatedVisibility(
                    visible = !isCreatingGroup,
                    enter = newGroupButtonEnterTransition(),
                    exit = newGroupButtonExitTransition(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                    ) {
                        NewGroupButton(
                            modifier = Modifier
                                .fillMaxWidth(),
                            enabled = true,
                            onClick = onCreateGroupClick,
                        )
                        Spacer(modifier = Modifier.height(height = 12.dp))
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    item {
                        NewChatLoadingState()
                    }
                }

                uiState.contacts.isEmpty() || !uiState.hasContactsPermission -> {
                    item {
                        NewChatEmptyState()
                    }
                }

                else -> {
                    itemsIndexed(
                        items = contacts,
                        key = { _, contact -> contact.id },
                        contentType = { _, _ ->
                            NEW_CHAT_CONTACT_CONTENT_TYPE
                        },
                    ) { index, contact ->
                        val bottomPadding = when {
                            index == lastContactIndex -> 0.dp
                            else -> 2.dp
                        }

                        NewChatContactRow(
                            modifier = Modifier
                                .padding(bottom = bottomPadding),
                            contact = contact,
                            enabled = contactSelectionEnabled,
                            isCreateGroupMode = isCreatingGroup,
                            isSelected = selectedGroupRecipientDestinations.contains(
                                contact.destination,
                            ),
                            onContactClick = onContactClick,
                            onContactLongClick = onContactLongClick,
                            onCreateGroupRecipientClick = onCreateGroupRecipientClick,
                            shape = newChatContactRowShape(
                                index = index,
                                totalCount = contacts.size,
                            ),
                            showResolvingIndicator = !isCreatingGroup &&
                                isResolvingConversationIndicatorVisible &&
                                resolvingRecipientDestination == contact.destination,
                        )
                    }
                }
            }

            if (uiState.isLoadingMore) {
                item {
                    NewChatLoadingMoreState()
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd),
            visible = showCreateGroupNextButton,
            enter = createGroupNextButtonEnterTransition(),
            exit = createGroupNextButtonExitTransition(),
        ) {
            CreateGroupNextButton(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = 8.dp, bottom = 8.dp),
                enabled = !uiState.isLoading && contactSelectionEnabled,
                isLoading = isResolvingConversationIndicatorVisible,
                onClick = onCreateGroupConfirmed,
            )
        }
    }
}

@Composable
private fun NewChatLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NewChatLoadingMoreState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size = 20.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun NewChatEmptyState() {
    Text(
        text = stringResource(id = R.string.contact_list_empty_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp),
    )
}

@Composable
private fun NewGroupButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    FilledTonalButton(
        modifier = modifier,
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(size = 18.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(
                alpha = 0.5f,
            ),
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                alpha = 0.5f,
            ),
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Group,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.size(size = 8.dp))
        Text(text = stringResource(id = R.string.conversation_new_group))
    }
}

@Composable
private fun CreateGroupNextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier
            .animateContentSize(
                animationSpec = defaultSpatialAnimationSpec(),
            )
            .testTag(NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(size = 18.dp),
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                nextButtonContentTransform()
            },
            label = "createGroupNextButtonContent",
        ) { isButtonLoading ->
            if (isButtonLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = 18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(id = R.string.next))
                    Spacer(modifier = Modifier.size(size = 8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewChatContactRow(
    modifier: Modifier = Modifier,
    contact: ConversationRecipient,
    shape: RoundedCornerShape,
    enabled: Boolean,
    isCreateGroupMode: Boolean,
    isSelected: Boolean,
    onContactClick: (String) -> Unit,
    onContactLongClick: (String) -> Unit,
    onCreateGroupRecipientClick: (String) -> Unit,
    showResolvingIndicator: Boolean,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "newChatContactSelection",
    )
    val containerColor by selectionTransition.animateContainerColor()
    val primaryTextColor by selectionTransition.animatePrimaryTextColor()
    val secondaryTextColor by selectionTransition.animateSecondaryTextColor()

    Row(
        modifier = Modifier
            .then(other = modifier)
            .fillMaxWidth()
            .testTag(newChatContactRowTestTag(contactId = contact.id))
            .semantics {
                selected = isSelected
            }
            .background(
                color = containerColor,
                shape = shape,
            )
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    when {
                        isCreateGroupMode -> {
                            onCreateGroupRecipientClick(contact.destination)
                        }

                        else -> {
                            onContactClick(contact.destination)
                        }
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    when {
                        isCreateGroupMode -> {
                            onCreateGroupRecipientClick(contact.destination)
                        }

                        else -> {
                            onContactLongClick(contact.destination)
                        }
                    }
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NewChatContactAvatar(
            contact = contact,
            isSelected = isSelected,
        )

        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Text(
                text = contact.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = primaryTextColor,
            )

            contact.secondaryText?.let { secondaryText ->
                Text(
                    text = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                )
            }
        }

        AnimatedVisibility(
            visible = showResolvingIndicator,
            enter = resolvingIndicatorEnterTransition(),
            exit = resolvingIndicatorExitTransition(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(size = 20.dp)
                    .testTag(NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG),
                strokeWidth = 2.dp,
            )
        }
    }
}

private fun newChatContactRowShape(
    index: Int,
    totalCount: Int,
): RoundedCornerShape {
    return when {
        totalCount <= 1 -> SingleContactShape
        index == 0 -> TopContactShape
        index == totalCount - 1 -> BottomContactShape
        else -> MiddleContactShape
    }
}

@Composable
private fun NewChatContactAvatar(
    contact: ConversationRecipient,
    isSelected: Boolean,
) {
    val avatarScale by rememberContactAvatarScale(
        isSelected = isSelected,
    )

    AnimatedContent(
        targetState = isSelected,
        transitionSpec = {
            contactAvatarContentTransform()
        },
        label = "newChatContactAvatar",
    ) { isSelectedState ->
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = avatarScale
                scaleY = avatarScale
            },
        ) {
            when {
                isSelectedState -> {
                    SelectedContactAvatar()
                }

                contact.photoUri == null -> {
                    NewChatContactTextAvatar(
                        contact = contact,
                    )
                }

                else -> {
                    AsyncImage(
                        model = contact.photoUri,
                        contentDescription = contact.displayName,
                        modifier = Modifier
                            .size(size = 40.dp)
                            .clip(shape = CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedContactAvatar(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size = 40.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun NewChatContactTextAvatar(
    modifier: Modifier = Modifier,
    contact: ConversationRecipient,
) {
    val label = remember(contact.displayName, contact.destination) {
        contactAvatarLabel(contact = contact)
    }

    Box(
        modifier = modifier
            .size(size = 40.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

private fun contactAvatarLabel(contact: ConversationRecipient): String {
    val labelSource = contact.displayName.ifBlank { contact.destination }
    val firstCharacter = labelSource.firstOrNull() ?: '?'

    return firstCharacter.uppercaseChar().toString()
}

private fun newGroupButtonEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = defaultEffectsAnimationSpec(),
    ) + slideInVertically(
        animationSpec = defaultSpatialAnimationSpec(),
        initialOffsetY = { fullHeight ->
            -fullHeight / 4
        },
    )
}

private fun newGroupButtonExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = fastEffectsAnimationSpec(),
    ) + shrinkVertically(
        animationSpec = defaultSpatialAnimationSpec(),
        shrinkTowards = Alignment.Top,
    )
}

private fun createGroupNextButtonEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = defaultEffectsAnimationSpec(),
    ) + slideInVertically(
        animationSpec = defaultSpatialAnimationSpec(),
        initialOffsetY = { fullHeight ->
            fullHeight / 2
        },
    ) + scaleIn(
        animationSpec = defaultSpatialAnimationSpec(),
        initialScale = 0.9f,
    )
}

private fun createGroupNextButtonExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = fastEffectsAnimationSpec(),
    ) + slideOutVertically(
        animationSpec = defaultSpatialAnimationSpec(),
        targetOffsetY = { fullHeight ->
            fullHeight / 2
        },
    ) + scaleOut(
        animationSpec = defaultSpatialAnimationSpec(),
        targetScale = 0.9f,
    )
}

private fun nextButtonContentTransform(): ContentTransform {
    return (fadeIn(
        animationSpec = defaultEffectsAnimationSpec(),
    ) + scaleIn(
        animationSpec = defaultSpatialAnimationSpec(),
        initialScale = 0.9f,
    )).togetherWith(
        fadeOut(
            animationSpec = fastEffectsAnimationSpec(),
        ) + scaleOut(
            animationSpec = defaultSpatialAnimationSpec(),
            targetScale = 0.9f,
        ),
    )
}

private fun resolvingIndicatorEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = defaultEffectsAnimationSpec(),
    ) + scaleIn(
        animationSpec = defaultSpatialAnimationSpec(),
        initialScale = 0.8f,
    )
}

private fun resolvingIndicatorExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = fastEffectsAnimationSpec(),
    ) + scaleOut(
        animationSpec = defaultSpatialAnimationSpec(),
        targetScale = 0.8f,
    )
}

private fun contactAvatarContentTransform(): ContentTransform {
    return (fadeIn(
        animationSpec = defaultEffectsAnimationSpec(),
    ) + scaleIn(
        animationSpec = defaultSpatialAnimationSpec(),
        initialScale = 0.8f,
    )).togetherWith(
        fadeOut(
            animationSpec = fastEffectsAnimationSpec(),
        ) + scaleOut(
            animationSpec = defaultSpatialAnimationSpec(),
            targetScale = 0.8f,
        ),
    )
}

@Composable
private fun rememberContactAvatarScale(
    isSelected: Boolean,
): State<Float> {
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "newChatContactAvatarScale",
    )

    return selectionTransition.animateFloat(
        transitionSpec = {
            defaultSpatialAnimationSpec()
        },
        label = "newChatContactAvatarScaleValue",
        targetValueByState = { isAvatarSelected ->
            when {
                isAvatarSelected -> 1f
                else -> 0.9f
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animateContainerColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            contactSelectionAnimationSpec()
        },
        label = "newChatContactContainerColor",
        targetValueByState = { isContactSelected ->
            when {
                isContactSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.background
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animatePrimaryTextColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            contactSelectionAnimationSpec()
        },
        label = "newChatContactPrimaryTextColor",
        targetValueByState = { isContactSelected ->
            when {
                isContactSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        },
    )
}

@Composable
private fun Transition<Boolean>.animateSecondaryTextColor(): State<Color> {
    return animateColor(
        transitionSpec = {
            contactSelectionAnimationSpec()
        },
        label = "newChatContactSecondaryTextColor",
        targetValueByState = { isContactSelected ->
            when {
                isContactSelected -> {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                }

                else -> {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            }
        },
    )
}

private fun <T> contactSelectionAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing,
    )
}

private fun <T> defaultEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing,
    )
}

private fun <T> fastEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )
}

private fun <T> defaultSpatialAnimationSpec(): FiniteAnimationSpec<T> {
    return spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

@Composable
private fun NewChatScreenPreviewContent(
    uiState: RecipientPickerUiState,
    isCreatingGroup: Boolean = false,
    isResolvingConversation: Boolean = false,
    isResolvingConversationIndicatorVisible: Boolean = false,
    resolvingRecipientDestination: String? = null,
    selectedGroupRecipientDestinations: ImmutableList<String> = persistentListOf(),
) {
    AppTheme {
        NewChatScreenContent(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            isCreatingGroup = isCreatingGroup,
            isResolvingConversation = isResolvingConversation,
            isResolvingConversationIndicatorVisible = isResolvingConversationIndicatorVisible,
            onContactClick = {},
            onContactLongClick = {},
            onCreateGroupClick = {},
            onCreateGroupConfirmed = {},
            onCreateGroupRecipientClick = {},
            onLoadMore = {},
            onQueryChanged = {},
            resolvingRecipientDestination = resolvingRecipientDestination,
            selectedGroupRecipientDestinations = selectedGroupRecipientDestinations,
        )
    }
}
