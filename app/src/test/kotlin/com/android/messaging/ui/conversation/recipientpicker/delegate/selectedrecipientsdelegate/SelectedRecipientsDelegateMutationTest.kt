package com.android.messaging.ui.conversation.recipientpicker.delegate.selectedrecipientsdelegate

import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SelectedRecipientsDelegateMutationTest : BaseSelectedRecipientsDelegateTest() {

    @Test
    fun replaceWith_fromEmpty_setsStateToSingleRecipient() {
        val delegate = createDelegate()
        val replacement = recipient(destination = "+15550001")

        delegate.replaceWith(recipient = replacement)

        assertEquals(listOf(replacement), delegate.state.value.toList())
        assertEquals(listOf(replacement), persistedRecipients())
    }

    @Test
    fun replaceWith_whenRecipientsExist_discardsPreviousSelection() {
        val delegate = createDelegate(
            initialRecipients = listOf(
                recipient(destination = "+15550001"),
                recipient(destination = "+15550002"),
            ),
        )

        delegate.replaceWith(recipient = recipient(destination = "+15550003"))

        assertEquals(listOf("+15550003"), delegate.state.value.map { it.destination })
        assertEquals(listOf("+15550003"), persistedRecipients()?.map { it.destination })
    }

    @Test
    fun clear_whenNotEmpty_emptiesStateAndPersistsEmptyList() {
        val delegate = createDelegate(
            initialRecipients = listOf(recipient(destination = "+15550001")),
        )

        delegate.clear()

        assertTrue(delegate.state.value.isEmpty())
        assertTrue(persistedRecipients()?.isEmpty() == true)
    }

    @Test
    fun clear_whenAlreadyEmpty_doesNotPersist() {
        val delegate = createDelegate()

        delegate.clear()

        assertTrue(delegate.state.value.isEmpty())
        assertNull(persistedRecipients())
        verify(exactly = 0) {
            savedStateHandle.set(SELECTED_RECIPIENTS_KEY, any<ArrayList<SelectedRecipient>>())
        }
    }

    @Test
    fun removeWhere_matchingPredicate_removesMatchesAndPersistsRemainder() {
        val delegate = createDelegate(
            initialRecipients = listOf(
                recipient(destination = "+15550001"),
                recipient(destination = "+15550002"),
                recipient(destination = "+15550003"),
            ),
        )

        delegate.removeWhere { it.destination == "+15550002" }

        assertEquals(
            listOf("+15550001", "+15550003"),
            delegate.state.value.map { it.destination },
        )
        assertEquals(
            listOf("+15550001", "+15550003"),
            persistedRecipients()?.map { it.destination },
        )
    }

    @Test
    fun removeWhere_predicateMatchesNothing_doesNotMutateOrPersist() {
        val delegate = createDelegate(
            initialRecipients = listOf(
                recipient(destination = "+15550001"),
                recipient(destination = "+15550002"),
            ),
        )

        delegate.removeWhere { it.destination == "+19999999" }

        assertEquals(
            listOf("+15550001", "+15550002"),
            delegate.state.value.map { it.destination },
        )
        verify(exactly = 0) {
            savedStateHandle.set(SELECTED_RECIPIENTS_KEY, any<ArrayList<SelectedRecipient>>())
        }
    }

    @Test
    fun removeWhere_predicateMatchesAll_emptiesStateAndPersistsEmptyList() {
        val delegate = createDelegate(
            initialRecipients = listOf(
                recipient(destination = "+15550001"),
                recipient(destination = "+15550002"),
            ),
        )

        delegate.removeWhere { true }

        assertTrue(delegate.state.value.isEmpty())
        assertTrue(persistedRecipients()?.isEmpty() == true)
    }
}
