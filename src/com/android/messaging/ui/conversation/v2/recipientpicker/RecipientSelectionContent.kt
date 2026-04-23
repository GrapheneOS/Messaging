@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.android.messaging.ui.conversation.v2.recipientpicker

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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.ui.conversation.v2.recipientpicker.model.RecipientPickerListItem

private val contactCornerRadius = 18.dp
private val contactMiddleCornerRadius = 2.dp
private val searchFieldShape = RoundedCornerShape(size = 22.dp)
private val topContactShape = RoundedCornerShape(
    topStart = contactCornerRadius,
    topEnd = contactCornerRadius,
    bottomStart = contactMiddleCornerRadius,
    bottomEnd = contactMiddleCornerRadius,
)
private val bottomContactShape = RoundedCornerShape(
    topStart = contactMiddleCornerRadius,
    topEnd = contactMiddleCornerRadius,
    bottomStart = contactCornerRadius,
    bottomEnd = contactCornerRadius,
)
private val middleContactShape = RoundedCornerShape(size = contactMiddleCornerRadius)
private val singleContactShape = RoundedCornerShape(size = contactCornerRadius)

private const val CONTACTS_LOAD_MORE_THRESHOLD = 10
private const val RECIPIENT_CONTACT_CONTENT_TYPE = "recipient_contact"

@Composable
internal fun RecipientSelectionContent(
    uiState: RecipientSelectionContentUiState,
    strings: RecipientSelectionStrings,
    rowDecorators: RecipientSelectionRowDecorators,
    onRecipientClick: (RecipientPickerListItem) -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
    onPrimaryActionClick: () -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onRecipientLongClick: ((RecipientPickerListItem) -> Unit)? = null,
    topListContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(height = 16.dp))

            RecipientSelectionQueryField(
                query = uiState.picker.query,
                enabled = uiState.isQueryEnabled,
                prefixText = strings.queryPrefixText,
                placeholderText = strings.queryPlaceholderText,
                onQueryChanged = onQueryChanged,
            )

            Spacer(modifier = Modifier.height(height = 12.dp))

            RecipientSelectionContactsContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                rowDecorators = rowDecorators,
                onLoadMore = onLoadMore,
                onPrimaryActionClick = onPrimaryActionClick,
                onRecipientClick = onRecipientClick,
                onRecipientLongClick = onRecipientLongClick,
                topListContent = topListContent,
            )
        }
    }
}

@Composable
private fun RecipientSelectionQueryField(
    query: String,
    enabled: Boolean,
    prefixText: String,
    placeholderText: String,
    onQueryChanged: (String) -> Unit,
) {
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChanged,
        enabled = enabled,
        singleLine = true,
        shape = searchFieldShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPrefixColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        prefix = {
            Text(
                modifier = Modifier.padding(end = 12.dp),
                text = prefixText,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        placeholder = {
            Text(text = placeholderText)
        },
    )
}

@Composable
private fun RecipientSelectionContactsContent(
    uiState: RecipientSelectionContentUiState,
    rowDecorators: RecipientSelectionRowDecorators,
    onLoadMore: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    onRecipientClick: (RecipientPickerListItem) -> Unit,
    onRecipientLongClick: ((RecipientPickerListItem) -> Unit)?,
    modifier: Modifier = Modifier,
    topListContent: (@Composable () -> Unit)? = null,
) {
    val pickerUiState = uiState.picker
    val primaryAction = uiState.primaryAction
    val lastContactIndex = pickerUiState.items.lastIndex
    val listState = rememberLazyListState()

    val animatedListBottomPadding by animateDpAsState(
        targetValue = when {
            primaryAction != null -> 100.dp
            else -> 16.dp
        },
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        label = "recipientSelectionListBottomPadding",
    )

    LaunchedEffect(
        listState,
        pickerUiState.canLoadMore,
        pickerUiState.isLoading,
        pickerUiState.isLoadingMore,
        pickerUiState.items.size,
    ) {
        snapshotFlow {
            val lastVisibleIndex = listState
                .layoutInfo
                .visibleItemsInfo
                .lastOrNull()
                ?.index
                ?: -1

            lastVisibleIndex >= lastContactIndex - CONTACTS_LOAD_MORE_THRESHOLD
        }.collect { shouldLoadMore ->
            if (
                shouldLoadMore &&
                pickerUiState.canLoadMore &&
                !pickerUiState.isLoading &&
                !pickerUiState.isLoadingMore
            ) {
                onLoadMore()
            }
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = animatedListBottomPadding),
        ) {
            topListContent?.let {
                item {
                    topListContent()
                }
            }

            when {
                pickerUiState.isLoading -> {
                    item {
                        RecipientSelectionLoadingState()
                    }
                }

                pickerUiState.items.isEmpty() -> {
                    item {
                        RecipientSelectionEmptyState()
                    }
                }

                else -> {
                    itemsIndexed(
                        items = pickerUiState.items,
                        key = { _, item -> item.id },
                        contentType = { _, _ -> RECIPIENT_CONTACT_CONTENT_TYPE },
                    ) { index, item ->
                        val bottomPadding = when {
                            index == lastContactIndex -> 0.dp
                            else -> 2.dp
                        }

                        RecipientSelectionContactRow(
                            modifier = Modifier.padding(bottom = bottomPadding),
                            item = item,
                            enabled = primaryAction?.isLoading != true,
                            isSelected = uiState.selectedRecipientDestinations.contains(
                                item.destination,
                            ),
                            onClick = {
                                onRecipientClick(item)
                            },
                            onLongClick = onRecipientLongClick?.let { callback ->
                                {
                                    callback(item)
                                }
                            },
                            rowTestTag = rowDecorators.recipientRowTestTag(item),
                            shape = recipientSelectionContactRowShape(
                                index = index,
                                totalCount = pickerUiState.items.size,
                            ),
                            showTrailingIndicator = rowDecorators
                                .showRecipientTrailingIndicator(
                                    item,
                                ),
                            trailingIndicatorTestTag = rowDecorators.trailingIndicatorTestTag,
                        )
                    }
                }
            }

            if (pickerUiState.isLoadingMore) {
                item {
                    RecipientSelectionLoadingMoreState()
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd),
            visible = primaryAction != null,
            enter = recipientSelectionPrimaryActionEnterTransition(),
            exit = recipientSelectionPrimaryActionExitTransition(),
        ) {
            RecipientSelectionPrimaryActionButton(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(end = 8.dp, bottom = 8.dp),
                enabled = primaryAction?.isEnabled ?: false,
                isLoading = primaryAction?.isLoading ?: false,
                text = primaryAction?.text.orEmpty(),
                testTag = primaryAction?.testTag,
                onClick = onPrimaryActionClick,
            )
        }
    }
}

@Composable
private fun RecipientSelectionLoadingState() {
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
private fun RecipientSelectionLoadingMoreState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(size = 20.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun RecipientSelectionEmptyState() {
    Text(
        modifier = Modifier
            .padding(vertical = 24.dp, horizontal = 4.dp),
        text = stringResource(id = R.string.contact_list_empty_text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RecipientSelectionPrimaryActionButton(
    enabled: Boolean,
    isLoading: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val taggedModifier = when {
        testTag != null -> modifier.testTag(testTag)
        else -> modifier
    }

    Button(
        modifier = taggedModifier
            .animateContentSize(
                animationSpec = recipientSelectionSpatialAnimationSpec(),
            ),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(size = 18.dp),
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                recipientSelectionPrimaryActionContentTransform()
            },
            label = "recipientSelectionPrimaryActionButtonContent",
        ) { isButtonLoading ->
            when {
                isButtonLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size = 18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = text)

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
}

@Composable
private fun RecipientSelectionContactRow(
    item: RecipientPickerListItem,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    rowTestTag: String,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showTrailingIndicator: Boolean = false,
    trailingIndicatorTestTag: String? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "recipientSelectionContactSelection",
    )

    val containerColor by selectionTransition.animateContainerColor()
    val primaryTextColor by selectionTransition.animatePrimaryTextColor()
    val secondaryTextColor by selectionTransition.animateSecondaryTextColor()

    Row(
        modifier = Modifier
            .then(other = modifier)
            .fillMaxWidth()
            .testTag(rowTestTag)
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
                    onClick()
                },
                onLongClick = onLongClick?.let { callback ->
                    {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        callback()
                    }
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecipientSelectionContactAvatar(
            item = item,
            isSelected = isSelected,
        )

        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Text(
                text = recipientSelectionItemDisplayName(item = item),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = primaryTextColor,
            )

            item.secondaryText?.let { secondaryText ->
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
            visible = showTrailingIndicator,
            enter = recipientSelectionTrailingIndicatorEnterTransition(),
            exit = recipientSelectionTrailingIndicatorExitTransition(),
        ) {
            CircularProgressIndicator(
                modifier = when {
                    trailingIndicatorTestTag != null -> {
                        Modifier
                            .size(size = 20.dp)
                            .testTag(trailingIndicatorTestTag)
                    }

                    else -> {
                        Modifier
                            .size(size = 20.dp)
                    }
                },
                strokeWidth = 2.dp,
            )
        }
    }
}

private fun recipientSelectionContactRowShape(
    index: Int,
    totalCount: Int,
): RoundedCornerShape {
    return when {
        totalCount <= 1 -> singleContactShape
        index == 0 -> topContactShape
        index == totalCount - 1 -> bottomContactShape
        else -> middleContactShape
    }
}

@Composable
private fun RecipientSelectionContactAvatar(
    item: RecipientPickerListItem,
    isSelected: Boolean,
) {
    val avatarScale by rememberRecipientSelectionContactAvatarScale(
        isSelected = isSelected,
    )

    AnimatedContent(
        targetState = isSelected,
        transitionSpec = {
            recipientSelectionAvatarContentTransform()
        },
        label = "recipientSelectionContactAvatar",
    ) { isSelectedState ->
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = avatarScale
                scaleY = avatarScale
            },
        ) {
            when {
                isSelectedState -> {
                    RecipientSelectionSelectedAvatar()
                }

                recipientSelectionPhotoUri(item) == null -> {
                    RecipientSelectionTextAvatar(item)
                }

                else -> {
                    AsyncImage(
                        modifier = Modifier
                            .size(size = 40.dp)
                            .clip(shape = CircleShape),
                        model = recipientSelectionPhotoUri(item),
                        contentDescription = recipientSelectionItemDisplayName(item),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipientSelectionSelectedAvatar(
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
private fun RecipientSelectionTextAvatar(
    item: RecipientPickerListItem,
    modifier: Modifier = Modifier,
) {
    val displayName = recipientSelectionItemDisplayName(item = item)
    val label = remember(displayName, item.destination) {
        recipientSelectionAvatarLabel(
            displayName = displayName,
            destination = item.destination,
        )
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

@Composable
private fun recipientSelectionItemDisplayName(
    item: RecipientPickerListItem,
): String {
    return when (item) {
        is RecipientPickerListItem.Contact -> item.recipient.displayName
        is RecipientPickerListItem.SyntheticPhone -> {
            stringResource(
                id = R.string.contact_list_send_to_text,
                item.rawQuery,
            )
        }
    }
}

private fun recipientSelectionPhotoUri(item: RecipientPickerListItem): String? {
    return when (item) {
        is RecipientPickerListItem.Contact -> item.recipient.photoUri
        is RecipientPickerListItem.SyntheticPhone -> null
    }
}

private fun recipientSelectionAvatarLabel(
    displayName: String,
    destination: String,
): String {
    val labelSource = displayName.ifBlank { destination }
    val firstCharacter = labelSource.firstOrNull() ?: '?'

    return firstCharacter.uppercaseChar().toString()
}

private fun recipientSelectionPrimaryActionEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = recipientSelectionDefaultEffectsAnimationSpec(),
    ) + slideInVertically(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        initialOffsetY = { fullHeight ->
            fullHeight / 2
        },
    ) + scaleIn(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        initialScale = 0.9f,
    )
}

private fun recipientSelectionPrimaryActionExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = recipientSelectionFastEffectsAnimationSpec(),
    ) + slideOutVertically(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        targetOffsetY = { fullHeight ->
            fullHeight / 2
        },
    ) + scaleOut(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        targetScale = 0.9f,
    )
}

private fun recipientSelectionPrimaryActionContentTransform(): ContentTransform {
    return recipientSelectionFadeAndScaleContentTransform(
        scale = 0.9f,
    )
}

private fun recipientSelectionTrailingIndicatorEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = recipientSelectionDefaultEffectsAnimationSpec(),
    ) + scaleIn(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        initialScale = 0.8f,
    )
}

private fun recipientSelectionTrailingIndicatorExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = recipientSelectionFastEffectsAnimationSpec(),
    ) + scaleOut(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        targetScale = 0.8f,
    )
}

private fun recipientSelectionAvatarContentTransform(): ContentTransform {
    return recipientSelectionFadeAndScaleContentTransform(
        scale = 0.8f,
    )
}

private fun recipientSelectionFadeAndScaleContentTransform(scale: Float): ContentTransform {
    val enterTransition = fadeIn(
        animationSpec = recipientSelectionDefaultEffectsAnimationSpec(),
    ) + scaleIn(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        initialScale = scale,
    )
    val exitTransition = fadeOut(
        animationSpec = recipientSelectionFastEffectsAnimationSpec(),
    ) + scaleOut(
        animationSpec = recipientSelectionSpatialAnimationSpec(),
        targetScale = scale,
    )

    return enterTransition.togetherWith(exitTransition)
}

@Composable
private fun rememberRecipientSelectionContactAvatarScale(
    isSelected: Boolean,
): State<Float> {
    val selectionTransition = updateTransition(
        targetState = isSelected,
        label = "recipientSelectionContactAvatarScale",
    )

    return selectionTransition.animateFloat(
        transitionSpec = {
            recipientSelectionSpatialAnimationSpec()
        },
        label = "recipientSelectionContactAvatarScaleValue",
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
            recipientSelectionSelectionAnimationSpec()
        },
        label = "recipientSelectionContactContainerColor",
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
            recipientSelectionSelectionAnimationSpec()
        },
        label = "recipientSelectionContactPrimaryTextColor",
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
            recipientSelectionSelectionAnimationSpec()
        },
        label = "recipientSelectionContactSecondaryTextColor",
        targetValueByState = { isContactSelected ->
            when {
                isContactSelected -> {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                }

                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        },
    )
}

private fun <T> recipientSelectionSelectionAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing,
    )
}

private fun <T> recipientSelectionDefaultEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 200,
        easing = LinearOutSlowInEasing,
    )
}

private fun <T> recipientSelectionFastEffectsAnimationSpec(): FiniteAnimationSpec<T> {
    return tween(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )
}

private fun <T> recipientSelectionSpatialAnimationSpec(): FiniteAnimationSpec<T> {
    return spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}
