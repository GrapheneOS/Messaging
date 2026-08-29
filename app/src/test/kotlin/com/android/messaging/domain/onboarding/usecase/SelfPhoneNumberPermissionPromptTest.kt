package com.android.messaging.domain.onboarding.usecase

import com.android.messaging.data.onboarding.store.SelfPhoneNumberPermissionStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfPhoneNumberPermissionPromptTest {

    @Test
    fun consume_whenNeverAsked_promptsAndRecordsTheAsk() {
        val store = mockStore(isGranted = false, isRequested = false)

        assertTrue(SelfPhoneNumberPermissionPromptImpl(store).consume())

        verify(exactly = 1) {
            store.markRequested()
        }
    }

    @Test
    fun consume_whenAlreadyAsked_doesNotPromptAgain() {
        val store = mockStore(isGranted = false, isRequested = true)

        assertFalse(
            "the permission is optional, so a user who declined it once is not asked again",
            SelfPhoneNumberPermissionPromptImpl(store).consume(),
        )
    }

    @Test
    fun consume_whenPermissionAlreadyGranted_doesNotPrompt() {
        val store = mockStore(isGranted = true, isRequested = false)

        assertFalse(SelfPhoneNumberPermissionPromptImpl(store).consume())

        verify(exactly = 0) {
            store.markRequested()
        }
    }

    private fun mockStore(
        isGranted: Boolean,
        isRequested: Boolean,
    ): SelfPhoneNumberPermissionStore {
        return mockk<SelfPhoneNumberPermissionStore>(relaxUnitFun = true).also {
            every { it.isGranted() } returns isGranted
            every { it.isRequested() } returns isRequested
        }
    }
}
