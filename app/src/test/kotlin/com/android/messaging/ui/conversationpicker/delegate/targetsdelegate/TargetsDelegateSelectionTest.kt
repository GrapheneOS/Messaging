package com.android.messaging.ui.conversationpicker.delegate.targetsdelegate

import app.cash.turbine.test
import com.android.messaging.testutil.TEST_CONTACT_DESTINATION
import com.android.messaging.testutil.contactTarget
import com.android.messaging.testutil.conversationTarget
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TargetsDelegateSelectionTest : BaseTargetsDelegateTest() {

    @Test
    fun toggleSelection_addsTarget() = runTest {
        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.toggleSelection(
            conversationTarget(
                normalizedDestination = TEST_CONTACT_DESTINATION,
            ),
        )

        val selection = delegate.state.value.selection
        assertEquals(setOf("dest:$TEST_CONTACT_DESTINATION"), selection.selectedIds)
        assertEquals(1, selection.selectedTargets.size)
    }

    @Test
    fun toggleSelection_calledTwiceForSameTarget_removesIt() = runTest {
        val delegate = createDelegate()
        val target = conversationTarget(
            normalizedDestination = TEST_CONTACT_DESTINATION,
        )

        delegate.toggleSelection(target)
        assertEquals(listOf(target), delegate.currentSelectedTargets())

        delegate.toggleSelection(target)

        assertTrue(delegate.currentSelectedTargets().isEmpty())
    }

    @Test
    fun toggleSelection_dedupesAcrossConversationAndContactWithSameDestination() = runTest {
        val delegate = createDelegate()

        delegate.toggleSelection(
            conversationTarget(
                normalizedDestination = TEST_CONTACT_DESTINATION,
            ),
        )
        delegate.toggleSelection(contactTarget())

        assertTrue(delegate.currentSelectedTargets().isEmpty())
    }

    @Test
    fun toggleSelection_usesConversationKeyWhenDestinationIsNull() = runTest {
        val delegate = createDelegate()
        delegate.bind(backgroundScope)
        runCurrent()

        delegate.toggleSelection(
            conversationTarget(
                conversationId = "7",
                normalizedDestination = null,
            ),
        )

        assertEquals(setOf("conversation:7"), delegate.state.value.selection.selectedIds)
    }

    @Test
    fun clearSelection_removesAllSelectedTargets() = runTest {
        val delegate = createDelegate()

        delegate.toggleSelection(
            conversationTarget(
                normalizedDestination = TEST_CONTACT_DESTINATION,
            ),
        )
        delegate.clearSelection()

        assertTrue(delegate.currentSelectedTargets().isEmpty())
    }

    @Test
    fun currentSelectedTargets_returnsSelectedTargets() = runTest {
        val delegate = createDelegate()
        val target = conversationTarget(
            normalizedDestination = TEST_CONTACT_DESTINATION,
        )

        delegate.toggleSelection(target)

        assertEquals(listOf(target), delegate.currentSelectedTargets())
    }

    @Test
    fun selectedIds_flowEmitsSelectionIds() = runTest {
        val delegate = createDelegate()

        delegate.toggleSelection(
            conversationTarget(
                normalizedDestination = TEST_CONTACT_DESTINATION,
            ),
        )

        delegate.selectedIds.test {
            assertEquals(setOf("dest:$TEST_CONTACT_DESTINATION"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recentsChange_prunesConversationNoLongerAvailable() = runTest {
        val source = givenRecentsSource()
        val delegate = createDelegate()
        delegate.bind(backgroundScope)

        source.emit(
            persistentListOf(
                shareTargetConversation(
                    conversationId = "1",
                    name = "One",
                ),
                shareTargetConversation(
                    conversationId = "2",
                    name = "Two",
                ),
            ),
        )
        runCurrent()

        delegate.toggleSelection(
            conversationTarget(
                conversationId = "1",
                normalizedDestination = null,
            ),
        )
        source.emit(
            persistentListOf(
                shareTargetConversation(
                    conversationId = "2",
                    name = "Two",
                ),
            ),
        )
        runCurrent()

        assertTrue(delegate.state.value.selection.selectedIds.isEmpty())
    }

    @Test
    fun recentsChange_keepsConversationStillAvailable() = runTest {
        val source = givenRecentsSource()
        val delegate = createDelegate()
        delegate.bind(backgroundScope)

        source.emit(
            persistentListOf(
                shareTargetConversation(
                    conversationId = "1",
                    name = "One",
                ),
            ),
        )
        runCurrent()

        delegate.toggleSelection(
            conversationTarget(
                conversationId = "1",
                normalizedDestination = null,
            ),
        )
        source.emit(
            persistentListOf(
                shareTargetConversation(
                    conversationId = "1",
                    name = "One",
                ),
            ),
        )
        runCurrent()

        assertEquals(setOf("conversation:1"), delegate.state.value.selection.selectedIds)
    }

    @Test
    fun recentsChange_keepsSelectedContact() = runTest {
        val source = givenRecentsSource()
        val delegate = createDelegate()
        delegate.bind(backgroundScope)

        source.emit(
            persistentListOf(
                shareTargetConversation(
                    conversationId = "1",
                    name = "One",
                ),
            ),
        )
        runCurrent()

        delegate.toggleSelection(contactTarget(destination = "+15550009"))
        source.emit(persistentListOf())
        runCurrent()

        assertEquals(setOf("dest:+15550009"), delegate.state.value.selection.selectedIds)
    }
}
