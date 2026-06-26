package com.android.messaging.ui.conversationlist.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceAnimationTrackerTest {

    private val tracker = AppearanceAnimationTracker()

    @Test
    fun computeEntering_firstFrame_hasNoEnteringConversations() {
        val entering = tracker.computeEntering(
            setOf("a", "b"),
            isListAtTop = true,
            excludedConversationIds = emptySet(),
        )

        assertTrue(entering.isEmpty())
    }

    @Test
    fun computeEntering_afterCommit_marksOnlyAddedConversations() {
        tracker.commitFrame(setOf("a", "b"))

        val entering = tracker.computeEntering(
            setOf("a", "b", "c"),
            isListAtTop = true,
            excludedConversationIds = emptySet(),
        )

        assertEquals(setOf("c"), entering.keys)
    }

    @Test
    fun computeEntering_whenListNotAtTop_marksNoEnteringConversations() {
        tracker.commitFrame(setOf("a", "b"))

        val entering = tracker.computeEntering(
            setOf("a", "b", "c"),
            isListAtTop = false,
            excludedConversationIds = emptySet(),
        )

        assertTrue(entering.isEmpty())
    }

    @Test
    fun computeEntering_excludedConversation_marksNoEnteringToken() {
        tracker.commitFrame(setOf("a", "b"))

        val entering = tracker.computeEntering(
            setOf("a", "b", "c"),
            isListAtTop = true,
            excludedConversationIds = setOf("c"),
        )

        assertTrue(entering.isEmpty())
    }

    @Test
    fun onAnimationFinished_withActiveToken_clearsToken() {
        tracker.commitFrame(setOf("a"))

        val token = tracker.commitFrame(setOf("a", "b")).getValue("b")
        tracker.onAnimationFinished("b", token)

        assertNull(tracker.tokenFor("b", emptyMap()))
    }

    @Test
    fun tokenFor_afterFinish_doesNotReplayWhileStillEntering() {
        tracker.commitFrame(setOf("a"))

        val entering = tracker.commitFrame(setOf("a", "b"))
        val token = entering.getValue("b")
        tracker.onAnimationFinished("b", token)

        assertNull(tracker.tokenFor("b", entering))
    }

    @Test
    fun tokenFor_afterReentry_returnsFreshToken() {
        tracker.commitFrame(setOf("a"))

        val firstToken = tracker.commitFrame(setOf("a", "b")).getValue("b")
        tracker.onAnimationFinished("b", firstToken)
        tracker.commitFrame(setOf("a"))

        val reentering = tracker.commitFrame(setOf("a", "b"))
        val secondToken = reentering.getValue("b")
        assertSame(secondToken, tracker.tokenFor("b", reentering))
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
        val entering = computeEntering(
            conversationIds,
            isListAtTop = true,
            excludedConversationIds = emptySet(),
        )
        commit(
            currentConversationIds = conversationIds,
            enteringTokens = entering,
        )
        return entering
    }
}
