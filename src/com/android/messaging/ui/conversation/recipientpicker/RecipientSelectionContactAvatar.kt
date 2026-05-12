package com.android.messaging.ui.conversation.recipientpicker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.messaging.R
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerListItem

@Composable
internal fun RecipientSelectionContactAvatar(
    item: RecipientPickerListItem,
    isSelected: Boolean,
) {
    val avatarScale by rememberRecipientSelectionContactAvatarScale(
        isSelected = isSelected,
    )

    AnimatedContent(
        targetState = isSelected,
        transitionSpec = {
            (
                fadeIn(
                    animationSpec = tween(durationMillis = 200),
                ) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    initialScale = 0.8f,
                )
                ).togetherWith(
                fadeOut(
                    animationSpec = tween(durationMillis = 150),
                ) + scaleOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    targetScale = 0.8f,
                ),
            )
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

                recipientSelectionPhotoUri(item = item) == null -> {
                    RecipientSelectionTextAvatar(item = item)
                }

                else -> {
                    AsyncImage(
                        modifier = Modifier
                            .size(size = 40.dp)
                            .clip(shape = CircleShape),
                        model = recipientSelectionPhotoUri(item = item),
                        contentDescription = recipientSelectionItemDisplayName(item = item),
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
    val avatarSourceText = recipientSelectionAvatarSourceText(item = item)
    val label = remember(displayName, avatarSourceText) {
        recipientSelectionAvatarLabel(
            displayName = displayName,
            destination = avatarSourceText,
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
internal fun recipientSelectionItemDisplayName(
    item: RecipientPickerListItem,
): String {
    return when (item) {
        is RecipientPickerListItem.Contact -> item.contact.displayName
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
        is RecipientPickerListItem.Contact -> item.contact.photoUri
        is RecipientPickerListItem.SyntheticPhone -> null
    }
}

private fun recipientSelectionAvatarSourceText(item: RecipientPickerListItem): String {
    return when (item) {
        is RecipientPickerListItem.Contact -> {
            item.contact.destinations.firstOrNull()?.value.orEmpty()
        }

        is RecipientPickerListItem.SyntheticPhone -> item.destination
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
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
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
