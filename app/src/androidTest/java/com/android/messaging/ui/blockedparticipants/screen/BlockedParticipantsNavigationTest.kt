package com.android.messaging.ui.blockedparticipants.screen

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.android.messaging.R
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsNavEvent
import com.android.messaging.ui.blockedparticipants.screen.support.BlockedParticipantsTestBase
import com.android.messaging.ui.blockedparticipants.screen.support.loadedState
import org.junit.Assert.assertEquals
import org.junit.Test

internal class BlockedParticipantsNavigationTest : BlockedParticipantsTestBase() {

    @Test
    fun backClick_invokesOnNavigateBack() {
        renderScreen(loadedState())

        composeTestRule
            .onNodeWithContentDescription(string(R.string.back))
            .performClick()

        assertEquals(1, onNavigateBackCalls.size)
    }

    @Test
    fun closeAfterLastUnblock_invokesOnNavigateBack() {
        renderScreen(loadedState())

        emitNavEvent(BlockedParticipantsNavEvent.CloseAfterLastUnblock)

        assertEquals(1, onNavigateBackCalls.size)
    }
}
