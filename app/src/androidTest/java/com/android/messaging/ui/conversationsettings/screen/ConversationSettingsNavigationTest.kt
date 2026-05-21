package com.android.messaging.ui.conversationsettings.screen

import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsNavEvent
import com.android.messaging.ui.conversationsettings.screen.support.ConversationSettingsTestBase
import com.android.messaging.ui.conversationsettings.screen.support.FINISH_RESULT_CODE
import com.android.messaging.ui.conversationsettings.screen.support.PARTICIPANT_CONVERSATION_ID
import com.android.messaging.ui.conversationsettings.screen.support.ROOT_CONVERSATION_ID
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationSettingsNavigationTest : ConversationSettingsTestBase() {

    @Test
    fun resume_callsRefreshState() {
        renderScreen()

        verify(atLeast = 1) { screenModel.refreshState() }
    }

    @Test
    fun initialRender_appliesRootConversationId() {
        renderScreen()

        verify(atLeast = 1) { screenModel.setConversationId(ROOT_CONVERSATION_ID) }
    }

    @Test
    fun openParticipantInfo_appliesParticipantConversationId() {
        renderScreen()

        emitNavEvent(
            ConversationSettingsNavEvent.OpenParticipantInfo(
                conversationId = PARTICIPANT_CONVERSATION_ID,
            ),
        )

        verify(exactly = 1) {
            screenModel.setConversationId(PARTICIPANT_CONVERSATION_ID)
        }
    }

    @Test
    fun closeAfterArchive_fromRoot_navigatesBackWithFinishResult() {
        renderScreen()

        emitNavEvent(ConversationSettingsNavEvent.CloseAfterArchive)

        assertEquals(listOf(FINISH_RESULT_CODE), onNavigateBackCalls)
    }

    @Test
    fun systemBack_fromParticipantRoute_returnsToRoot() {
        renderScreen()

        emitNavEvent(
            ConversationSettingsNavEvent.OpenParticipantInfo(
                conversationId = PARTICIPANT_CONVERSATION_ID,
            ),
        )

        composeTestRule.runOnIdle {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        verify(atLeast = 2) { screenModel.setConversationId(ROOT_CONVERSATION_ID) }
        assertEquals(listOf<Int?>(), onNavigateBackCalls)
    }
}
