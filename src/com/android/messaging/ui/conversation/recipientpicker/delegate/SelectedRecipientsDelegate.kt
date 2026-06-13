package com.android.messaging.ui.conversation.recipientpicker.delegate

import androidx.lifecycle.SavedStateHandle
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientToggleOutcome
import com.android.messaging.ui.recipientselection.model.picker.SelectedRecipient
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface SelectedRecipientsDelegate {
    val state: StateFlow<ImmutableList<SelectedRecipient>>

    fun toggle(
        recipient: SelectedRecipient,
        canAdd: (newSize: Int) -> Boolean,
    ): RecipientToggleOutcome

    fun replaceWith(recipient: SelectedRecipient)

    fun clear()

    fun removeWhere(predicate: (SelectedRecipient) -> Boolean)
}

internal class SelectedRecipientsDelegateImpl @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : SelectedRecipientsDelegate {

    private val _state = MutableStateFlow(restoreSelectedRecipients())
    override val state = _state.asStateFlow()

    override fun toggle(
        recipient: SelectedRecipient,
        canAdd: (newSize: Int) -> Boolean,
    ): RecipientToggleOutcome {
        val currentRecipients = _state.value
        val destination = recipient.destination
        val isAlreadySelected = currentRecipients.any { current ->
            current.destination == destination
        }

        return when {
            isAlreadySelected -> {
                applyRecipients(
                    recipients = currentRecipients
                        .filterNot { current -> current.destination == destination }
                        .toImmutableList(),
                )

                RecipientToggleOutcome.Removed
            }

            !canAdd(currentRecipients.size + 1) -> RecipientToggleOutcome.OverLimit

            else -> {
                applyRecipients(
                    recipients = (currentRecipients + recipient).toImmutableList(),
                )

                RecipientToggleOutcome.Added
            }
        }
    }

    override fun replaceWith(recipient: SelectedRecipient) {
        applyRecipients(persistentListOf(recipient))
    }

    override fun clear() {
        if (_state.value.isNotEmpty()) {
            applyRecipients(persistentListOf())
        }
    }

    override fun removeWhere(predicate: (SelectedRecipient) -> Boolean) {
        val currentRecipients = _state.value
        val nextRecipients = currentRecipients
            .filterNot(predicate)
            .toImmutableList()

        if (nextRecipients.size != currentRecipients.size) {
            applyRecipients(nextRecipients)
        }
    }

    private fun applyRecipients(recipients: ImmutableList<SelectedRecipient>) {
        _state.value = recipients
        savedStateHandle[SELECTED_RECIPIENTS_KEY] = ArrayList(recipients)
    }

    private fun restoreSelectedRecipients(): ImmutableList<SelectedRecipient> {
        return savedStateHandle
            .get<ArrayList<SelectedRecipient>>(SELECTED_RECIPIENTS_KEY)
            ?.toImmutableList()
            ?: persistentListOf()
    }

    private companion object {
        private const val SELECTED_RECIPIENTS_KEY = "selected_recipients"
    }
}
