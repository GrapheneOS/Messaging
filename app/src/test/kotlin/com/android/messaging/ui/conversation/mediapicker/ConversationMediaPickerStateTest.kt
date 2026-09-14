package com.android.messaging.ui.conversation.mediapicker

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMediaPickerStateTest {

    @Test
    fun openShowReviewAndClose_updateStateAsExpected() {
        val state = ConversationMediaPickerState(
            isOpen = false,
            captureMode = ConversationCaptureMode.Photo,
            isReviewRequested = false,
            reviewContentUri = null,
            reviewRequestSequence = 0,
            selectedMediaIds = setOf(SELECTED_MEDIA_ID),
            shouldRestoreKeyboard = false,
        )

        assertTrue(state.isSelected(SELECTED_MEDIA_ID))

        state.open()
        assertTrue(state.isOpen)
        assertTrue(state.isReviewRequested)

        state.showReview(REMOTE_CONTENT_URI)
        assertEquals(REMOTE_CONTENT_URI, state.reviewContentUri)
        assertEquals(1, state.reviewRequestSequence)

        state.clearReview()
        assertFalse(state.isReviewRequested)
        assertEquals(null, state.reviewContentUri)

        state.close()
        assertFalse(state.isOpen)
        assertFalse(state.isReviewRequested)
        assertEquals(null, state.reviewContentUri)
        assertFalse(state.isSelected(SELECTED_MEDIA_ID))
    }

    @Test
    fun saver_roundTripsAllStateFields() {
        val state = ConversationMediaPickerState(
            isOpen = true,
            captureMode = ConversationCaptureMode.Video,
            isReviewRequested = true,
            reviewContentUri = REMOTE_CONTENT_URI,
            reviewRequestSequence = 7,
            selectedMediaIds = setOf(SELECTED_MEDIA_ID, SECOND_SELECTED_MEDIA_ID),
            shouldRestoreKeyboard = true,
        )
        val saverScope = SaverScope { true }

        val savedState = with(ConversationMediaPickerState.Saver) {
            with(saverScope) {
                save(state)
            }
        }

        assertNotNull(savedState)

        val restoredState = with(ConversationMediaPickerState.Saver) {
            restore(savedState!!)
        }

        assertNotNull(restoredState)
        assertEquals(ConversationCaptureMode.Video, restoredState!!.captureMode)
        assertTrue(restoredState.isOpen)
        assertTrue(restoredState.isReviewRequested)
        assertEquals(REMOTE_CONTENT_URI, restoredState.reviewContentUri)
        assertEquals(7, restoredState.reviewRequestSequence)
        assertTrue(restoredState.isSelected(SELECTED_MEDIA_ID))
        assertTrue(restoredState.isSelected(SECOND_SELECTED_MEDIA_ID))
        assertTrue(restoredState.shouldRestoreKeyboard)
    }

    private companion object {
        private const val REMOTE_CONTENT_URI = "content://media/external/images/media/123"
        private const val SELECTED_MEDIA_ID = "selected-media-1"
        private const val SECOND_SELECTED_MEDIA_ID = "selected-media-2"
    }
}
