package com.android.messaging.domain.conversation.usecase.draft

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.conversation.model.metadata.ConversationMetadata
import com.android.messaging.data.conversation.model.send.ConversationSendData
import com.android.messaging.datamodel.MessageTextStats
import com.android.messaging.datamodel.data.ConversationParticipantsData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.sms.MmsSmsUtils
import com.android.messaging.sms.MmsUtils
import com.android.messaging.testutil.TEST_SELF_SUB_ID as SELF_SUB_ID
import com.android.messaging.testutil.createConversationMetadata
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetConversationDraftSendProtocolImplTest {

    @Before
    fun setUp() {
        unmockkAll()
        mockkStatic(MmsSmsUtils::class)
        mockkStatic(MmsUtils::class)
        mockkConstructor(MessageTextStats::class)

        every {
            MmsSmsUtils.getRequireMmsForEmailAddress(any(), any())
        } returns false
        every { MmsUtils.groupMmsEnabled(any()) } returns false
        every {
            anyConstructed<MessageTextStats>().updateMessageTextStats(any(), any())
        } just runs
        every { anyConstructed<MessageTextStats>().messageLengthRequiresMms } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_whenDraftHasAttachment_returnsMms() {
        val result = createUseCase().invoke(
            draft = ConversationDraft(
                attachments = persistentListOf(
                    ConversationDraftAttachment(
                        contentType = "image/png",
                        contentUri = "content://image/1",
                    ),
                ),
            ),
            sendData = createSendData(),
        )

        assertEquals(ConversationDraftSendProtocol.MMS, result)
    }

    @Test
    fun invoke_whenDraftHasSubject_returnsMms() {
        val result = createUseCase().invoke(
            draft = ConversationDraft(
                messageText = "Hello",
                subjectText = "Subject",
            ),
            sendData = createSendData(),
        )

        assertEquals(ConversationDraftSendProtocol.MMS, result)
    }

    @Test
    fun invoke_whenGroupConversationRequiresMms_returnsMms() {
        every { MmsUtils.groupMmsEnabled(SELF_SUB_ID) } returns true

        val result = createUseCase().invoke(
            draft = ConversationDraft(
                messageText = "Hello",
            ),
            sendData = createSendData(
                metadata = createConversationMetadata(isGroupConversation = true),
            ),
        )

        assertEquals(ConversationDraftSendProtocol.MMS, result)
        verify(exactly = 1) {
            MmsUtils.groupMmsEnabled(SELF_SUB_ID)
        }
    }

    @Test
    fun invoke_whenEmailAddressRequiresMms_returnsMms() {
        every {
            MmsSmsUtils.getRequireMmsForEmailAddress(true, SELF_SUB_ID)
        } returns true

        val result = createUseCase().invoke(
            draft = ConversationDraft(
                messageText = "Hello",
            ),
            sendData = createSendData(
                metadata = createConversationMetadata(includeEmailAddress = true),
            ),
        )

        assertEquals(ConversationDraftSendProtocol.MMS, result)
        verify(exactly = 1) {
            MmsSmsUtils.getRequireMmsForEmailAddress(true, SELF_SUB_ID)
        }
    }

    @Test
    fun invoke_whenMessageLengthRequiresMms_returnsMms() {
        every { anyConstructed<MessageTextStats>().messageLengthRequiresMms } returns true

        val result = createUseCase().invoke(
            draft = ConversationDraft(
                messageText = "A long message",
            ),
            sendData = createSendData(),
        )

        assertEquals(ConversationDraftSendProtocol.MMS, result)
        verify(exactly = 1) {
            anyConstructed<MessageTextStats>().updateMessageTextStats(
                SELF_SUB_ID,
                "A long message",
            )
        }
    }

    @Test
    fun invoke_whenNoMmsConditionMatches_returnsSms() {
        val result = createUseCase().invoke(
            draft = ConversationDraft(
                messageText = "Hello",
            ),
            sendData = createSendData(),
        )

        assertEquals(ConversationDraftSendProtocol.SMS, result)
    }

    @Test
    fun invoke_whenSelfParticipantIsMissing_usesDefaultSelfSubIdForTextStats() {
        val result = createUseCase().invoke(
            draft = ConversationDraft(
                messageText = "Hello",
            ),
            sendData = createSendData(selfParticipant = null),
        )

        assertEquals(ConversationDraftSendProtocol.SMS, result)
        verify(exactly = 1) {
            anyConstructed<MessageTextStats>().updateMessageTextStats(
                ParticipantData.DEFAULT_SELF_SUB_ID,
                "Hello",
            )
        }
    }

    private fun createUseCase(): GetConversationDraftSendProtocolImpl {
        return GetConversationDraftSendProtocolImpl()
    }

    private fun createSendData(
        metadata: ConversationMetadata = createConversationMetadata(),
        selfParticipant: ParticipantData? = ParticipantData.getSelfParticipant(SELF_SUB_ID),
    ): ConversationSendData {
        return ConversationSendData(
            metadata = metadata,
            participants = mockk<ConversationParticipantsData>(),
            selfParticipant = selfParticipant,
        )
    }
}
