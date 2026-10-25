package com.android.messaging.ui.conversation.messages.ui.text

import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLink
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLinkKind

@Stable
internal class ConversationTextSelectionState(
    val selection: SelectionState = SelectionState(),
    initialLinks: List<ConversationTextLink> = emptyList(),
) {
    var links: List<ConversationTextLink> by mutableStateOf(initialLinks)
        private set

    private var pendingLinks: List<ConversationTextLink>? = null
    private val textCoordinates = mutableMapOf<Any, LayoutCoordinates>()

    var selectMessage: () -> Unit = {}
    var enabled: Boolean = true
    var isTouchExplorationEnabled: Boolean by mutableStateOf(false)
    var bodyTextOffset: Int = 0
    var subjectCoordinates: LayoutCoordinates? = null
    var subjectTextLayout: TextLayoutResult? = null

    val hasSelection: Boolean
        get() = selection.selectedTexts.any { it.isNotEmpty() }

    val selectedLink: ConversationTextLink?
        get() = selectedConversationTextLink(selectedTexts = selection.selectedTexts, links = links)

    fun updateLinks(detectedLinks: List<ConversationTextLink>) {
        when {
            hasSelection -> pendingLinks = detectedLinks
            else -> links = detectedLinks
        }
    }

    fun applyPendingLinks() {
        if (!hasSelection) {
            pendingLinks?.let { links = it }
            pendingLinks = null
        }
    }

    fun updateCoordinates(key: Any, coordinates: LayoutCoordinates) {
        textCoordinates[key] = coordinates
    }

    fun removeCoordinates(key: Any) {
        textCoordinates.remove(key = key)
    }

    // The context menu keeps its last anchor once the selected text scrolls out of view.
    fun clearSelectionOutOfView() {
        val isVisible = textCoordinates.values.any { coordinates ->
            coordinates.isAttached && !coordinates.boundsInWindow().isEmpty
        }
        if (hasSelection && !isVisible) selection.clear()
    }

    fun containsText(positionInRoot: Offset): Boolean {
        return enabled && !isTouchExplorationEnabled && textCoordinates.values.any { coordinates ->
            coordinates.isAttached && coordinates.boundsInRoot().contains(positionInRoot)
        }
    }

    fun subjectWordAt(positionInRoot: Offset): TextRange? {
        val coordinates = subjectCoordinates
        val layout = subjectTextLayout
        if (coordinates == null || layout == null) return null
        return when {
            coordinates.isAttached && positionInRoot.y <= coordinates.boundsInRoot().bottom -> {
                val position = coordinates.localPositionOf(
                    sourceCoordinates = coordinates.findRootCoordinates(),
                    relativeToSource = positionInRoot,
                )
                layout.getWordBoundary(offset = layout.getOffsetForPosition(position = position))
            }
            else -> null
        }
    }

    companion object {
        val Saver: Saver<ConversationTextSelectionState, Any> = listSaver(
            save = { state ->
                val selection = with(SelectionState.Saver) { save(value = state.selection) }
                listOf(selection) + when {
                    state.hasSelection -> {
                        state.links.flatMap { link ->
                            listOf(
                                link.start,
                                link.end,
                                link.url,
                                link.kind.name,
                                link.originalText,
                            )
                        }
                    }

                    else -> emptyList()
                }
            },
            restore = { saved ->
                val selection = saved
                    .first()
                    ?.let { SelectionState.Saver.restore(value = it) }
                    ?: SelectionState()

                ConversationTextSelectionState(
                    selection = selection,
                    initialLinks = saved.drop(n = 1).chunked(size = 5).map { link ->
                        ConversationTextLink(
                            start = link[0] as Int,
                            end = link[1] as Int,
                            url = link[2] as String,
                            kind = ConversationTextLinkKind.valueOf(value = link[3] as String),
                            originalText = link[4] as String,
                        )
                    },
                )
            },
        )
    }
}
