package com.android.messaging.domain.conversation.usecase.participant

import com.android.messaging.datamodel.data.ContactPickerData
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanAddMoreConversationParticipantsImplTest {

    @Before
    fun setUp() {
        unmockkAll()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_delegatesToLegacyContactPickerLimitCheck() {
        val useCase = CanAddMoreConversationParticipantsImpl()
        mockkStatic(ContactPickerData::class)
        every { ContactPickerData.getCanAddMoreParticipants(0) } returns true
        every { ContactPickerData.getCanAddMoreParticipants(4) } returns true
        every { ContactPickerData.getCanAddMoreParticipants(5) } returns false

        assertTrue(useCase.invoke(participantCount = 0))
        assertTrue(useCase.invoke(participantCount = 4))
        assertFalse(useCase.invoke(participantCount = 5))

        verify(exactly = 1) {
            ContactPickerData.getCanAddMoreParticipants(0)
        }
        verify(exactly = 1) {
            ContactPickerData.getCanAddMoreParticipants(4)
        }
        verify(exactly = 1) {
            ContactPickerData.getCanAddMoreParticipants(5)
        }
    }
}
