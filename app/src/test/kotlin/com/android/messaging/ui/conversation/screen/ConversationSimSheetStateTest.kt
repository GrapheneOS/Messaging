package com.android.messaging.ui.conversation.screen

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ConversationSimSheetStateTest {

    @Test
    fun showAndDismiss_updateVisibility() {
        val state = ConversationSimSheetState()

        assertFalse(state.isVisible)

        state.show()
        assertTrue(state.isVisible)

        state.dismiss()
        assertFalse(state.isVisible)
    }

    @Test
    fun saver_roundTripsVisibility() {
        val state = ConversationSimSheetState()
        state.show()
        val saverScope = SaverScope { true }

        val savedState = with(ConversationSimSheetState.Saver) {
            with(saverScope) {
                save(state)
            }
        }

        assertNotNull(savedState)

        val restoredState = with(ConversationSimSheetState.Saver) {
            restore(savedState!!)
        }

        assertNotNull(restoredState)
        assertTrue(restoredState!!.isVisible)
    }
}
