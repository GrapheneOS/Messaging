package com.android.messaging.ui.conversation.messagedetails.mapper

import com.android.messaging.data.conversation.model.message.ConversationMessageDetails
import com.android.messaging.datamodel.data.ConversationMessageData
import com.android.messaging.ui.conversation.messagedetails.model.MessageDetailsUiState
import com.android.messaging.ui.conversation.messages.mapper.ConversationMessageUiModelMapper
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

internal class MessageDetailsUiStateMapperImplTest {

    private val conversationMessageUiModelMapper = mockk<ConversationMessageUiModelMapper>()

    private val mapper = MessageDetailsUiStateMapperImpl(
        conversationMessageUiModelMapper = conversationMessageUiModelMapper,
    )

    @Test
    fun map_withMessageAndDetails_buildsContentWithPreview() {
        val message = mockk<ConversationMessageData>()
        val preview = mockk<ConversationMessageUiModel>()

        every { conversationMessageUiModelMapper.map(data = message) } returns preview

        val result = mapper.map(
            message = message,
            details = details(),
        )

        val content = result as MessageDetailsUiState.Content
        assertSame(preview, content.preview)
    }

    @Test
    fun map_withNullMessage_returnsUnavailable() {
        val result = mapper.map(
            message = null,
            details = details(),
        )

        assertEquals(MessageDetailsUiState.Unavailable, result)
    }

    @Test
    fun map_withNullDetails_returnsUnavailable() {
        val result = mapper.map(
            message = mockk<ConversationMessageData>(),
            details = null,
        )

        assertEquals(MessageDetailsUiState.Unavailable, result)
    }

    private fun details(): ConversationMessageDetails {
        return ConversationMessageDetails(
            type = ConversationMessageDetails.Type.SMS,
            sender = null,
            recipients = persistentListOf(),
            sentTimestamp = null,
            receivedTimestamp = null,
            subject = null,
            priority = null,
            sizeBytes = null,
            subscriptionLabel = null,
            debug = null,
        )
    }
}
