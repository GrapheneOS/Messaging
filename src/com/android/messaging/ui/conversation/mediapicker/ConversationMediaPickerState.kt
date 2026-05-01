package com.android.messaging.ui.conversation.mediapicker

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize

internal enum class ConversationCaptureMode {
    Photo,
    Video,
}

@Parcelize
internal data class ConversationMediaPickerSavedState(
    val captureModeName: String,
    val isOpen: Boolean,
    val isReviewRequested: Boolean,
    val reviewContentUri: String?,
    val reviewRequestSequence: Int,
    val selectedMediaIds: List<String>,
    val shouldRestoreKeyboard: Boolean,
) : Parcelable

@Stable
internal class ConversationMediaPickerState(
    isOpen: Boolean,
    captureMode: ConversationCaptureMode,
    isReviewRequested: Boolean,
    reviewContentUri: String?,
    reviewRequestSequence: Int,
    selectedMediaIds: Set<String>,
    shouldRestoreKeyboard: Boolean,
) {
    var captureMode by mutableStateOf(captureMode)
    var isOpen by mutableStateOf(isOpen)
    var isReviewRequested by mutableStateOf(isReviewRequested)
    var reviewContentUri by mutableStateOf(reviewContentUri)
    var reviewRequestSequence by mutableIntStateOf(reviewRequestSequence)
    var shouldRestoreKeyboard by mutableStateOf(shouldRestoreKeyboard)

    private var selectedMediaIds by mutableStateOf(selectedMediaIds)

    fun clearSelection() {
        selectedMediaIds = emptySet()
    }

    fun isSelected(mediaId: String): Boolean {
        return selectedMediaIds.contains(mediaId)
    }

    fun open() {
        isReviewRequested = true
        isOpen = true
    }

    fun showReview(contentUri: String) {
        isReviewRequested = true
        reviewContentUri = contentUri
        reviewRequestSequence += 1
    }

    fun clearReview() {
        isReviewRequested = false
        reviewContentUri = null
    }

    fun updateCaptureMode(captureMode: ConversationCaptureMode) {
        this.captureMode = captureMode
    }

    fun close() {
        clearSelection()
        clearReview()
        isOpen = false
    }

    private fun toSavedState(): ConversationMediaPickerSavedState {
        return ConversationMediaPickerSavedState(
            captureModeName = captureMode.name,
            isOpen = isOpen,
            isReviewRequested = isReviewRequested,
            reviewContentUri = reviewContentUri,
            reviewRequestSequence = reviewRequestSequence,
            selectedMediaIds = selectedMediaIds.toList(),
            shouldRestoreKeyboard = shouldRestoreKeyboard,
        )
    }

    companion object {
        val Saver: Saver<ConversationMediaPickerState, ConversationMediaPickerSavedState> = Saver(
            save = { it.toSavedState() },
            restore = { restoredState ->
                ConversationMediaPickerState(
                    isOpen = restoredState.isOpen,
                    captureMode = restoredState.captureModeName.toConversationCaptureMode(),
                    isReviewRequested = restoredState.isReviewRequested,
                    reviewContentUri = restoredState.reviewContentUri,
                    reviewRequestSequence = restoredState.reviewRequestSequence,
                    selectedMediaIds = restoredState.selectedMediaIds.toSet(),
                    shouldRestoreKeyboard = restoredState.shouldRestoreKeyboard,
                )
            },
        )
    }
}

private fun String.toConversationCaptureMode(): ConversationCaptureMode {
    return ConversationCaptureMode
        .entries
        .firstOrNull { it.name == this }
        ?: ConversationCaptureMode.Photo
}

@Composable
internal fun rememberConversationMediaPickerState(): ConversationMediaPickerState {
    return rememberSaveable(saver = ConversationMediaPickerState.Saver) {
        ConversationMediaPickerState(
            isOpen = false,
            captureMode = ConversationCaptureMode.Photo,
            isReviewRequested = false,
            reviewContentUri = null,
            reviewRequestSequence = 0,
            selectedMediaIds = emptySet(),
            shouldRestoreKeyboard = false,
        )
    }
}
