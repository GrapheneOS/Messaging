package com.android.messaging.ui.conversationlist.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceAnimationTrackerTest {

    private val tracker = AppearanceAnimationTracker()

    @Test
    fun computeEntering_firstFrame_hasNoEnteringConversations() {
        val entering = tracker.computeEntering(setOf("a", "b"))

        assertTrue(entering.isEmpty())
    }

    @Test
    fun computeEntering_afterCommit_marksOnlyAddedConversations() {
        tracker.commitFrame(setOf("a", "b"))

        val entering = tracker.computeEntering(setOf("a", "b", "c"))

        assertEquals(setOf("c"), entering.keys)
    }

    @Test
    fun onAnimationFinished_withActiveToken_clearsToken() {
        tracker.commitFrame(setOf("a"))

        val token = tracker.commitFrame(setOf("a", "b")).getValue("b")
        tracker.onAnimationFinished("b", token)

        assertNull(tracker.tokenFor("b", emptyMap()))
    }

    @Test
    fun onAnimationFinished_withStaleToken_keepsActiveToken() {
        tracker.commitFrame(setOf("a"))

        val staleToken = tracker.commitFrame(setOf("a", "b")).getValue("b")
        tracker.commitFrame(setOf("a"))

        val activeToken = tracker.commitFrame(setOf("a", "b")).getValue("b")
        tracker.onAnimationFinished("b", staleToken)

        assertSame(activeToken, tracker.tokenFor("b", emptyMap()))
    }

    private fun AppearanceAnimationTracker.commitFrame(
        conversationIds: Set<String>,
    ): Map<String, AppearanceAnimationToken> {
        val entering = computeEntering(conversationIds)
        commit(
            currentConversationIds = conversationIds,
            enteringTokens = entering,
        )
        return entering
    }
}
