package com.android.messaging.ui.conversation.messages.ui.message

import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveConversationMessageSimDisplayNameTest {

    @Test
    fun returnsNullWhenSingleSubscription() {
        val message = message(selfParticipantId = SIM_1_ID, isIncoming = false)

        val result = resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = null,
            simDisplayNameByParticipantId = mapOf(SIM_1_ID to SIM_1_NAME),
        )

        assertNull(result)
    }

    @Test
    fun returnsNullWhenSelfParticipantIdMissingFromMap() {
        val message = message(selfParticipantId = "unknown", isIncoming = false)

        val result = resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = null,
            simDisplayNameByParticipantId = simDisplayNames,
        )

        assertNull(result)
    }

    @Test
    fun returnsDisplayNameWhenLastMessageInConversation() {
        val message = message(selfParticipantId = SIM_2_ID, isIncoming = false)

        val result = resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = null,
            simDisplayNameByParticipantId = simDisplayNames,
        )

        assertEquals(SIM_2_NAME, result)
    }

    @Test
    fun returnsDisplayNameWhenNextMessageUsesDifferentSim() {
        val message = message(selfParticipantId = SIM_1_ID, isIncoming = false)
        val below = message(selfParticipantId = SIM_2_ID, isIncoming = false)

        val result = resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = below,
            simDisplayNameByParticipantId = simDisplayNames,
        )

        assertEquals(SIM_1_NAME, result)
    }

    @Test
    fun returnsDisplayNameWhenNextMessageHasOppositeDirection() {
        val message = message(selfParticipantId = SIM_1_ID, isIncoming = false)
        val below = message(selfParticipantId = SIM_1_ID, isIncoming = true)

        val result = resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = below,
            simDisplayNameByParticipantId = simDisplayNames,
        )

        assertEquals(SIM_1_NAME, result)
    }

    @Test
    fun returnsNullWhenInsideContiguousSimRun() {
        val message = message(selfParticipantId = SIM_1_ID, isIncoming = false)
        val below = message(selfParticipantId = SIM_1_ID, isIncoming = false)

        val result = resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = below,
            simDisplayNameByParticipantId = simDisplayNames,
        )

        assertNull(result)
    }

    @Test
    fun returnsDisplayNameForMmsDownloadInsideContiguousSimRun() {
        val message = message(
            selfParticipantId = SIM_1_ID,
            isIncoming = true,
            mmsDownload = mmsDownload(),
        )
        val below = message(selfParticipantId = SIM_1_ID, isIncoming = true)

        val result = resolveConversationMessageSimDisplayName(
            message = message,
            messageBelow = below,
            simDisplayNameByParticipantId = simDisplayNames,
        )

        assertEquals(SIM_1_NAME, result)
    }

    private fun message(
        selfParticipantId: String?,
        isIncoming: Boolean,
        mmsDownload: MmsDownloadUiModel? = null,
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = "id-${selfParticipantId.orEmpty()}-$isIncoming",
            conversationId = "conversation",
            text = "text",
            parts = persistentListOf(
                ConversationMessagePartUiModel.Text(text = "text"),
            ),
            sentTimestamp = 0L,
            receivedTimestamp = 0L,
            displayTimestamp = 0L,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = isIncoming,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactId = ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED,
            senderContactLookupKey = null,
            senderNormalizedDestination = null,
            senderParticipantId = null,
            selfParticipantId = selfParticipantId,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = false,
            canDownloadMessage = false,
            canForwardMessage = false,
            canResendMessage = false,
            canSaveAttachments = false,
            mmsDownload = mmsDownload,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }

    private fun mmsDownload(): MmsDownloadUiModel {
        return MmsDownloadUiModel(
            state = MmsDownloadUiModel.State.AwaitingManualDownload,
            sizeBytes = 0L,
            expiryTimestamp = 0L,
        )
    }

    private companion object {
        private const val SIM_1_ID = "self-sim-1"
        private const val SIM_2_ID = "self-sim-2"
        private const val SIM_1_NAME = "SIM 1"
        private const val SIM_2_NAME = "SIM 2"

        private val simDisplayNames = mapOf(
            SIM_1_ID to SIM_1_NAME,
            SIM_2_ID to SIM_2_NAME,
        )
    }
}
