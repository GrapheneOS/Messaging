package com.android.messaging.ui.conversation.recipientpicker.delegate.selectedrecipientsdelegate

import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SelectedRecipientsDelegateToggleTest : BaseSelectedRecipientsDelegateTest() {

    @Test
    fun toggle_newRecipientWithinLimit_addsRecipientAndReturnsAdded() {
        val delegate = createDelegate()
        val added = recipient(destination = "+15550001")

        val outcome = delegate.toggle(recipient = added, canAdd = { true })

        assertEquals(RecipientToggleOutcome.Added, outcome)
        assertEquals(listOf(added), delegate.state.value.toList())
        assertEquals(listOf(added), persistedRecipients())
    }

    @Test
    fun toggle_secondRecipient_appendsPreservingInsertionOrder() {
        val delegate = createDelegate(
            initialRecipients = listOf(recipient(destination = "+15550001")),
        )

        val outcome = delegate.toggle(
            recipient = recipient(destination = "+15550002"),
            canAdd = { true },
        )

        assertEquals(RecipientToggleOutcome.Added, outcome)
        assertEquals(
            listOf("+15550001", "+15550002"),
            delegate.state.value.map { it.destination },
        )
        assertEquals(
            listOf("+15550001", "+15550002"),
            persistedRecipients()?.map { it.destination },
        )
    }

    @Test
    fun toggle_alreadySelectedDestination_removesItAndReturnsRemoved() {
        val delegate = createDelegate(
            initialRecipients = listOf(
                recipient(destination = "+15550001"),
                recipient(destination = "+15550002"),
            ),
        )

        val outcome = delegate.toggle(
            recipient = recipient(destination = "+15550001"),
            canAdd = { true },
        )

        assertEquals(RecipientToggleOutcome.Removed, outcome)
        assertEquals(listOf("+15550002"), delegate.state.value.map { it.destination })
        assertEquals(listOf("+15550002"), persistedRecipients()?.map { it.destination })
    }

    @Test
    fun toggle_alreadySelectedDestination_doesNotConsultCanAdd() {
        val existing = recipient(destination = "+15550001")
        val delegate = createDelegate(initialRecipients = listOf(existing))
        val canAdd = mockk<(Int) -> Boolean>()

        val outcome = delegate.toggle(recipient = existing, canAdd = canAdd)

        assertEquals(RecipientToggleOutcome.Removed, outcome)
        verify(exactly = 0) { canAdd(any()) }
    }

    @Test
    fun toggle_sameDestinationDifferentFields_matchesByDestinationAndRemoves() {
        val delegate = createDelegate(
            initialRecipients = listOf(
                recipient(
                    destination = "+15550001",
                    label = "Alice",
                    displayDestination = "Alice Home",
                    photoUri = "content://photo/alice",
                ),
            ),
        )

        val outcome = delegate.toggle(
            recipient = recipient(
                destination = "+15550001",
                label = "Bob",
                displayDestination = "Bob Work",
                photoUri = null,
            ),
            canAdd = { true },
        )

        assertEquals(RecipientToggleOutcome.Removed, outcome)
        assertTrue(delegate.state.value.isEmpty())
    }

    @Test
    fun toggle_canAddRejectsProspectiveSize_returnsOverLimitWithoutMutating() {
        val delegate = createDelegate(
            initialRecipients = listOf(recipient(destination = "+15550001")),
        )

        val outcome = delegate.toggle(
            recipient = recipient(destination = "+15550002"),
            canAdd = { false },
        )

        assertEquals(RecipientToggleOutcome.OverLimit, outcome)
        assertEquals(listOf("+15550001"), delegate.state.value.map { it.destination })
        verify(exactly = 0) {
            savedStateHandle[SELECTED_RECIPIENTS_KEY] = any<ArrayList<SelectedRecipient>>()
        }
    }

    @Test
    fun toggle_addingToExistingSelection_passesProspectiveSizeToCanAdd() {
        val delegate = createDelegate(
            initialRecipients = listOf(
                recipient(destination = "+15550001"),
                recipient(destination = "+15550002"),
            ),
        )
        val canAdd = mockk<(Int) -> Boolean>()
        every { canAdd(any()) } returns true

        delegate.toggle(recipient = recipient(destination = "+15550003"), canAdd = canAdd)

        verify { canAdd(3) }
    }
}
