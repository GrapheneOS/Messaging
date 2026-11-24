package com.android.messaging.ui.host

import androidx.lifecycle.ViewModel
import com.android.messaging.data.devicepolicy.SmsRestrictionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface DeviceOwnerSmsBlockModel {
    val isSmsDisallowed: StateFlow<Boolean>

    fun onHostResumed()
}

@HiltViewModel
internal class DeviceOwnerSmsBlockViewModel @Inject constructor(
    private val smsRestrictionChecker: SmsRestrictionChecker,
) : ViewModel(),
    DeviceOwnerSmsBlockModel {

    private val _isSmsDisallowed = MutableStateFlow(false)
    override val isSmsDisallowed: StateFlow<Boolean> = _isSmsDisallowed.asStateFlow()

    override fun onHostResumed() {
        _isSmsDisallowed.value = smsRestrictionChecker.isSmsDisallowed()
    }
}
