package com.android.messaging.ui.conversation.messages.ui.text

import android.content.ClipData
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLink
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLinkKind

@Composable
internal fun ConversationTextSelectionContainer(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val state = LocalConversationTextSelection.current
    val selection = state?.selection ?: rememberSelectionState()
    val copy = rememberConversationLinkCopyAction(state = state)
    val currentCopy = rememberUpdatedState(newValue = copy)
    val selectMessageLabel = stringResource(R.string.conversation_message_select_message)
    val clipboard = LocalClipboard.current
    val selectionClipboard = remember(clipboard, selection) {
        ConversationTextSelectionClipboard(clipboard = clipboard, selection = selection)
    }

    CompositionLocalProvider(LocalClipboard provides selectionClipboard) {
        SelectionContainer(
            state = selection,
            modifier = Modifier
                .appendTextContextMenuComponents {
                    if (state != null && state.hasSelection && enabled) {
                        currentCopy.value?.let { action ->
                            item(key = "copy-detected-content", label = action.label) {
                                action.action()
                                close()
                            }
                        }
                        item(key = "select-message", label = selectMessageLabel) {
                            close()
                            state.selectMessage()
                        }
                    }
                }
                // SelectionContainer clears a selection before it hides its toolbar, which would
                // otherwise redraw with only "Select all" while it fades out.
                .filterTextContextMenuComponents {
                    selection.selectedTexts.any { text -> text.isNotEmpty() }
                },
        ) {
            when {
                enabled -> content()
                else -> DisableSelection(content = content)
            }
        }
    }
}

// SelectionContainer copies its Texts joined by newlines, including the subject's empty part when
// a selection starts at the body's first character.
private class ConversationTextSelectionClipboard(
    private val clipboard: Clipboard,
    private val selection: SelectionState,
) : Clipboard by clipboard {
    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val clipData = clipEntry?.clipData
        val text = clipData?.takeIf { it.itemCount == 1 }?.getItemAt(0)?.text
        val startsWithEmptyPart = selection.selectedTexts.firstOrNull()?.isEmpty() == true
        val entry = when {
            clipData == null || text == null -> clipEntry
            startsWithEmptyPart && text.startsWith('\n') -> {
                val item = ClipData.Item(text.subSequence(1, text.length))
                ClipEntry(clipData = ClipData(clipData.description, item))
            }
            else -> clipEntry
        }
        clipboard.setClipEntry(clipEntry = entry)
    }
}

@Composable
private fun rememberConversationLinkCopyAction(
    state: ConversationTextSelectionState?,
): CustomAccessibilityAction? {
    val coordinator = LocalConversationTextSelectionCoordinator.current
    val selectedLink = state?.selectedLink
    val label = selectedLink?.let { link -> stringResource(id = link.kind.copyLabelRes()) }
    return remember(state, coordinator, selectedLink, label) {
        when {
            state == null || coordinator == null || selectedLink == null || label == null -> null
            else -> {
                CustomAccessibilityAction(label = label) {
                    val currentLink = state.selectedLink
                    val matchesDisplayedAction = currentLink != null && currentLink == selectedLink
                    if (matchesDisplayedAction) {
                        coordinator.copyText(text = selectedLink.clipboardText())
                        state.selection.clear()
                    }
                    matchesDisplayedAction
                }
            }
        }
    }
}

@Composable
internal fun conversationTextAccessibilityActions(
    hasText: Boolean,
): List<CustomAccessibilityAction> {
    val state = LocalConversationTextSelection.current
    val copyActions = rememberConversationLinkCopyActions(links = state?.links.orEmpty())
    val selectTextLabel = stringResource(R.string.conversation_message_select_text)
    val selectMessageLabel = stringResource(R.string.conversation_message_select_message)
    return when {
        state == null || !hasText || !state.enabled -> emptyList()
        else -> buildList {
            add(
                CustomAccessibilityAction(label = selectTextLabel) {
                    state.selection.selectAll()
                    true
                },
            )
            add(
                CustomAccessibilityAction(label = selectMessageLabel) {
                    state.selectMessage()
                    true
                },
            )
            addAll(copyActions)
        }
    }
}

@Composable
private fun rememberConversationLinkCopyActions(
    links: List<ConversationTextLink>,
): List<CustomAccessibilityAction> {
    val coordinator = LocalConversationTextSelectionCoordinator.current
    val resources = LocalResources.current
    return remember(links, coordinator, resources) {
        when (coordinator) {
            null -> emptyList()
            else -> links.distinctBy { it.clipboardText() }.map { link ->
                val label = resources.getString(link.kind.copyTargetLabelRes(), link.originalText)
                CustomAccessibilityAction(label = label) {
                    coordinator.copyText(text = link.clipboardText())
                    true
                }
            }
        }
    }
}

@StringRes
private fun ConversationTextLinkKind.copyLabelRes(): Int {
    return when (this) {
        ConversationTextLinkKind.Web -> R.string.conversation_copy_link
        ConversationTextLinkKind.Phone -> R.string.conversation_copy_phone_number
        ConversationTextLinkKind.Email -> R.string.conversation_copy_email_address
        ConversationTextLinkKind.Address -> R.string.conversation_copy_address
    }
}

@StringRes
private fun ConversationTextLinkKind.copyTargetLabelRes(): Int {
    return when (this) {
        ConversationTextLinkKind.Web -> R.string.conversation_copy_link_target
        ConversationTextLinkKind.Phone -> R.string.conversation_copy_phone_number_target
        ConversationTextLinkKind.Email -> R.string.conversation_copy_email_address_target
        ConversationTextLinkKind.Address -> R.string.conversation_copy_address_target
    }
}

/**
 * Keeps touch off the text for a screen reader, whose double-tap-and-hold long presses the middle
 * of the message to select it. The message's actions still select the text and copy each link.
 */
@Composable
internal fun ConversationSelectableTextTouchGuard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = LocalConversationTextSelection.current
    Box(modifier = modifier, propagateMinConstraints = true) {
        content()
        if (state?.isTouchExplorationEnabled == true) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {}
            )
        }
    }
}

@Composable
internal fun Modifier.conversationSelectableTextBounds(): Modifier {
    val state = LocalConversationTextSelection.current
    val coordinateKey = remember { Any() }
    DisposableEffect(state, coordinateKey) {
        onDispose { state?.removeCoordinates(key = coordinateKey) }
    }
    return onGloballyPositioned { coordinates ->
        state?.updateCoordinates(key = coordinateKey, coordinates = coordinates)
        state?.clearSelectionOutOfView()
    }
}
