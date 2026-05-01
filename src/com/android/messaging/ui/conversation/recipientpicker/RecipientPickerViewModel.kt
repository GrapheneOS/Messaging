package com.android.messaging.ui.conversation.recipientpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.ui.conversation.recipientpicker.delegate.RecipientPickerDelegate
import com.android.messaging.ui.conversation.recipientpicker.model.RecipientPickerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

internal interface RecipientPickerModel {
    val uiState: StateFlow<RecipientPickerUiState>

    fun onLoadMore()

    fun onExcludedDestinationsChanged(destinations: Set<String>)

    fun onQueryChanged(query: String)
}

@HiltViewModel
internal class RecipientPickerViewModel @Inject constructor(
    private val recipientPickerDelegate: RecipientPickerDelegate,
) : ViewModel(),
    RecipientPickerModel {

    override val uiState = recipientPickerDelegate.state

    init {
        recipientPickerDelegate.bind(scope = viewModelScope)
    }

    override fun onLoadMore() {
        recipientPickerDelegate.onLoadMore()
    }

    override fun onExcludedDestinationsChanged(destinations: Set<String>) {
        recipientPickerDelegate.onExcludedDestinationsChanged(destinations = destinations)
    }

    override fun onQueryChanged(query: String) {
        recipientPickerDelegate.onQueryChanged(query = query)
    }
}
