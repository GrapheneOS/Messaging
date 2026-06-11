package com.android.messaging.ui.conversation.recipientpicker

import com.android.messaging.ui.conversation.recipientpicker.delegate.RecipientPickerDelegate
import com.android.messaging.ui.conversation.recipientpicker.model.picker.RecipientPickerUiState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertSame
import org.junit.Test

class RecipientPickerViewModelTest {

    @Test
    fun init_bindsDelegateAndExposesState() {
        val delegate = createDelegate()

        val viewModel = RecipientPickerViewModel(
            recipientPickerDelegate = delegate,
        )

        assertSame(delegate.state, viewModel.uiState)
        verify(exactly = 1) {
            delegate.bind(scope = any())
        }
    }

    @Test
    fun onLoadMore_forwardsToDelegate() {
        val delegate = createDelegate()
        val viewModel = RecipientPickerViewModel(
            recipientPickerDelegate = delegate,
        )

        viewModel.onLoadMore()

        verify(exactly = 1) {
            delegate.onLoadMore()
        }
    }

    @Test
    fun onExcludedDestinationsChanged_forwardsToDelegate() {
        val delegate = createDelegate()
        val viewModel = RecipientPickerViewModel(
            recipientPickerDelegate = delegate,
        )

        viewModel.onExcludedDestinationsChanged(
            destinations = setOf("+15550100"),
        )

        verify(exactly = 1) {
            delegate.onExcludedDestinationsChanged(
                destinations = setOf("+15550100"),
            )
        }
    }

    @Test
    fun onQueryChanged_forwardsToDelegate() {
        val delegate = createDelegate()
        val viewModel = RecipientPickerViewModel(
            recipientPickerDelegate = delegate,
        )

        viewModel.onQueryChanged(query = "Ada")

        verify(exactly = 1) {
            delegate.onQueryChanged(query = "Ada")
        }
    }

    private fun createDelegate(): RecipientPickerDelegate {
        val delegate = mockk<RecipientPickerDelegate>(relaxed = true)
        every { delegate.state } returns MutableStateFlow(RecipientPickerUiState())
        every { delegate.bind(scope = any()) } just runs
        every { delegate.onLoadMore() } just runs
        every { delegate.onExcludedDestinationsChanged(destinations = any()) } just runs
        every { delegate.onQueryChanged(query = any()) } just runs

        return delegate
    }
}
