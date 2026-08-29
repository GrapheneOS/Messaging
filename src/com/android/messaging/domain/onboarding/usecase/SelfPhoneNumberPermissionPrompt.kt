package com.android.messaging.domain.onboarding.usecase

import com.android.messaging.data.onboarding.store.SelfPhoneNumberPermissionStore
import com.android.messaging.datamodel.ParticipantRefresh
import javax.inject.Inject

internal interface SelfPhoneNumberPermissionPrompt {

    /**
     * Whether the permission is still worth asking for, spending the single ask the user is owed.
     * Consuming before the dialog is shown rather than after it is answered keeps a process death
     * mid-prompt from asking again.
     */
    fun consume(): Boolean

    fun onGranted()
}

internal class SelfPhoneNumberPermissionPromptImpl @Inject constructor(
    private val store: SelfPhoneNumberPermissionStore,
) : SelfPhoneNumberPermissionPrompt {

    override fun consume(): Boolean {
        return when {
            store.isGranted() -> false
            store.isRequested() -> false
            else -> {
                store.markRequested()
                true
            }
        }
    }

    override fun onGranted() {
        ParticipantRefresh.refreshSelfParticipants()
    }
}
