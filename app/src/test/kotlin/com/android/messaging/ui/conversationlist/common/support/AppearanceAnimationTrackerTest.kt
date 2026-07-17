package com.android.messaging.ui.conversationlist.common.support

import com.android.messaging.data.conversation.model.ConversationId
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
            setOf(ConversationId("a"), ConversationId("b")),
            isListAtTop = true,
            excludedConversationIds = emptySet(),
        )

        assertTrue(entering.isEmpty())
    }

    @Test
    fun computeEntering_afterCommit_marksOnlyAddedConversations() {
        tracker.commitFrame(setOf(ConversationId("a"), ConversationId("b")))

        val entering = tracker.computeEntering(
            setOf(ConversationId("a"), ConversationId("b"), ConversationId("c")),
            isListAtTop = true,
            excludedConversationIds = emptySet(),
        )

        assertEquals(setOf(ConversationId("c")), entering.keys)
    }

    @Test
    fun computeEntering_whenListNotAtTop_marksNoEnteringConversations() {
        tracker.commitFrame(setOf(ConversationId("a"), ConversationId("b")))

        val entering = tracker.computeEntering(
            setOf(ConversationId("a"), ConversationId("b"), ConversationId("c")),
            isListAtTop = false,
            excludedConversationIds = emptySet(),
        )

        assertTrue(entering.isEmpty())
    }

    @Test
    fun computeEntering_excludedConversation_marksNoEnteringToken() {
        tracker.commitFrame(setOf(ConversationId("a"), ConversationId("b")))

        val entering = tracker.computeEntering(
            setOf(ConversationId("a"), ConversationId("b"), ConversationId("c")),
            isListAtTop = true,
            excludedConversationIds = setOf(ConversationId("c")),
        )

        assertTrue(entering.isEmpty())
    }

    @Test
    fun onAnimationFinished_withActiveToken_clearsToken() {
        tracker.commitFrame(setOf(ConversationId("a")))

        val token = tracker.commitFrame(
            setOf(ConversationId("a"), ConversationId("b"))
        ).getValue(ConversationId("b"))
        tracker.onAnimationFinished(ConversationId("b"), token)

        assertNull(tracker.tokenFor(ConversationId("b"), emptyMap()))
    }

    @Test
    fun tokenFor_afterFinish_doesNotReplayWhileStillEntering() {
        tracker.commitFrame(setOf(ConversationId("a")))

        val entering = tracker.commitFrame(setOf(ConversationId("a"), ConversationId("b")))
        val token = entering.getValue(ConversationId("b"))
        tracker.onAnimationFinished(ConversationId("b"), token)

        assertNull(tracker.tokenFor(ConversationId("b"), entering))
    }

    @Test
    fun tokenFor_afterReentry_returnsFreshToken() {
        tracker.commitFrame(setOf(ConversationId("a")))

        val firstToken = tracker.commitFrame(
            setOf(ConversationId("a"), ConversationId("b"))
        ).getValue(ConversationId("b"))
        tracker.onAnimationFinished(ConversationId("b"), firstToken)
        tracker.commitFrame(setOf(ConversationId("a")))

        val reentering = tracker.commitFrame(setOf(ConversationId("a"), ConversationId("b")))
        val secondToken = reentering.getValue(ConversationId("b"))
        assertSame(secondToken, tracker.tokenFor(ConversationId("b"), reentering))
    }

    @Test
    fun onAnimationFinished_withStaleToken_keepsActiveToken() {
        tracker.commitFrame(setOf(ConversationId("a")))

        val staleToken = tracker.commitFrame(
            setOf(ConversationId("a"), ConversationId("b"))
        ).getValue(ConversationId("b"))
        tracker.commitFrame(setOf(ConversationId("a")))

        val activeToken = tracker.commitFrame(
            setOf(ConversationId("a"), ConversationId("b"))
        ).getValue(ConversationId("b"))
        tracker.onAnimationFinished(ConversationId("b"), staleToken)

        assertSame(activeToken, tracker.tokenFor(ConversationId("b"), emptyMap()))
    }

    private fun AppearanceAnimationTracker.commitFrame(
        conversationIds: Set<ConversationId>,
    ): Map<ConversationId, AppearanceAnimationToken> {
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
