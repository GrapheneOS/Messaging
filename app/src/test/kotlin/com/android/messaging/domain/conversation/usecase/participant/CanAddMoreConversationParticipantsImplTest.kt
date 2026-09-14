package com.android.messaging.domain.conversation.usecase.participant

import com.android.messaging.sms.MmsConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanAddMoreConversationParticipantsImplTest {

    private val useCase = CanAddMoreConversationParticipantsImpl()

    @Before
    fun setUp() {
        mockkStatic(MmsConfig::class)
        every { MmsConfig.get(any()) } returns mockk {
            every { recipientLimit } returns RECIPIENT_LIMIT
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenParticipantCountIsBelowRecipientLimit_returnsTrue() {
        assertTrue(useCase.invoke(participantCount = RECIPIENT_LIMIT - 1))
    }

    @Test
    fun invoke_whenParticipantCountReachedRecipientLimit_returnsFalse() {
        assertFalse(useCase.invoke(participantCount = RECIPIENT_LIMIT))
    }
}

private const val RECIPIENT_LIMIT = 5
