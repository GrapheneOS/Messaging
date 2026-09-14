package com.android.messaging.ui.conversation.recipientpicker.delegate.selectedrecipientsdelegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.ui.conversation.recipientpicker.delegate.SelectedRecipientsDelegateImpl
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SelectedRecipientsDelegateRestorationTest : BaseSelectedRecipientsDelegateTest() {

    @Test
    fun state_whenNoSavedRecipients_startsEmpty() {
        val delegate = createDelegate()

        assertTrue(delegate.state.value.isEmpty())
    }

    @Test
    fun state_whenSavedRecipientsExist_restoresThemInOrder() {
        val saved = listOf(
            recipient(destination = "+15550001"),
            recipient(destination = "+15550002"),
        )
        val delegate = createDelegate(initialRecipients = saved)

        assertEquals(saved, delegate.state.value.toList())
    }

    @Test
    fun state_whenSavedListPersistedEmpty_startsEmpty() {
        savedStateHandle = spyk(
            SavedStateHandle(
                initialState = mapOf(SELECTED_RECIPIENTS_KEY to ArrayList<SelectedRecipient>()),
            ),
        )
        val delegate = SelectedRecipientsDelegateImpl(savedStateHandle = savedStateHandle)

        assertTrue(delegate.state.value.isEmpty())
    }

    @Test
    fun mutation_persistsUnderSelectedRecipientsKeyAsArrayList() {
        val delegate = createDelegate()
        val replacement = recipient(destination = "+15550001")

        delegate.replaceWith(recipient = replacement)

        val persisted = slot<Any>()
        verify { savedStateHandle.set(SELECTED_RECIPIENTS_KEY, capture(persisted)) }
        assertTrue(persisted.captured is ArrayList<*>)
        assertEquals(listOf(replacement), (persisted.captured as ArrayList<*>).toList())
    }
}
