package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.conversationMessageBubbleTestTag
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessage
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMessageAvatar
import com.android.messaging.ui.conversation.messages.ui.message.ConversationMmsDownloadBody
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Before
import org.junit.Rule

internal abstract class BaseConversationMessageRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    protected val onAttachmentClick = mockk<(String, String, String) -> Unit>(relaxed = true)
    protected val onAvatarClick = mockk<() -> Unit>(relaxed = true)
    protected val onDownloadClick = mockk<() -> Unit>(relaxed = true)
    protected val onExternalUriClick = mockk<(String) -> Unit>(relaxed = true)
    protected val onMessageClick = mockk<() -> Unit>(relaxed = true)
    protected val onMessageLongClick = mockk<() -> Unit>(relaxed = true)
    protected val onResendClick = mockk<() -> Unit>(relaxed = true)
    protected val onSimSelectorClick = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUp() {
        clearAllMocks()
    }

    protected fun setConversationMessageContent(
        message: ConversationMessageUiModel,
        isSelected: Boolean = false,
        isSelectionMode: Boolean = false,
        showIncomingParticipantIdentity: Boolean = true,
        simDisplayName: String? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationMessage(
                    message = message,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    showIncomingParticipantIdentity = showIncomingParticipantIdentity,
                    simDisplayName = simDisplayName,
                    onAttachmentClick = onAttachmentClick,
                    onExternalUriClick = onExternalUriClick,
                    onMessageClick = onMessageClick,
                    onMessageAvatarClick = onAvatarClick,
                    onMessageDownloadClick = onDownloadClick,
                    onMessageLongClick = onMessageLongClick,
                    onMessageResendClick = onResendClick,
                    onSimSelectorClick = onSimSelectorClick,
                )
            }
        }
    }

    protected fun setAvatarContent(message: ConversationMessageUiModel) {
        composeTestRule.setContent {
            AppTheme {
                ConversationMessageAvatar(
                    modifier = Modifier.testTag(tag = AVATAR_TAG),
                    message = message,
                    onClick = onAvatarClick,
                    onLongClick = onMessageLongClick,
                )
            }
        }
    }

    protected fun setMmsDownloadBodyContent(
        download: MmsDownloadUiModel,
        canDownloadMessage: Boolean,
        isSelected: Boolean = false,
        simDisplayName: String? = null,
    ) {
        composeTestRule.setContent {
            AppTheme {
                ConversationMmsDownloadBody(
                    download = download,
                    canDownloadMessage = canDownloadMessage,
                    isSelected = isSelected,
                    contentColor = Color.Black,
                    simDisplayName = simDisplayName,
                )
            }
        }
    }

    protected fun message(
        messageId: String = DEFAULT_MESSAGE_ID,
        text: String? = DEFAULT_BODY_TEXT,
        parts: ImmutableList<ConversationMessagePartUiModel> = persistentListOf(),
        status: ConversationMessageUiModel.Status =
            ConversationMessageUiModel.Status.Outgoing.Complete,
        isIncoming: Boolean = false,
        senderDisplayName: String? = null,
        senderAvatarUri: Uri? = null,
        senderContactId: Long = ParticipantData.PARTICIPANT_CONTACT_ID_NOT_RESOLVED,
        senderContactLookupKey: String? = null,
        senderNormalizedDestination: String? = null,
        senderParticipantId: String? = null,
        canClusterWithPrevious: Boolean = false,
        canClusterWithNext: Boolean = false,
        canDownloadMessage: Boolean = false,
        canResendMessage: Boolean = false,
        canSaveAttachments: Boolean = false,
        mmsDownload: MmsDownloadUiModel? = null,
        mmsSubject: String? = null,
        protocol: ConversationMessageUiModel.Protocol = ConversationMessageUiModel.Protocol.SMS,
    ): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = messageId,
            conversationId = CONVERSATION_ID,
            text = text,
            parts = parts,
            sentTimestamp = TIMESTAMP,
            receivedTimestamp = TIMESTAMP,
            displayTimestamp = TIMESTAMP,
            status = status,
            isIncoming = isIncoming,
            senderDisplayName = senderDisplayName,
            senderAvatarUri = senderAvatarUri,
            senderContactId = senderContactId,
            senderContactLookupKey = senderContactLookupKey,
            senderNormalizedDestination = senderNormalizedDestination,
            senderParticipantId = senderParticipantId,
            selfParticipantId = SELF_PARTICIPANT_ID,
            canClusterWithPrevious = canClusterWithPrevious,
            canClusterWithNext = canClusterWithNext,
            canCopyMessageToClipboard = !isIncoming,
            canDownloadMessage = canDownloadMessage,
            canForwardMessage = true,
            canResendMessage = canResendMessage,
            canSaveAttachments = canSaveAttachments,
            mmsDownload = mmsDownload,
            mmsSubject = mmsSubject,
            protocol = protocol,
        )
    }

    protected fun mmsDownload(
        state: MmsDownloadUiModel.State = MmsDownloadUiModel.State.AwaitingManualDownload,
        sizeBytes: Long = MMS_SIZE_BYTES,
        expiryTimestamp: Long = MMS_EXPIRY_TIMESTAMP,
        isSecondaryUser: Boolean = false,
    ): MmsDownloadUiModel {
        return MmsDownloadUiModel(
            state = state,
            sizeBytes = sizeBytes,
            expiryTimestamp = expiryTimestamp,
            isSecondaryUser = isSecondaryUser,
        )
    }

    protected fun imagePart(
        text: String? = null,
        contentType: String = IMAGE_CONTENT_TYPE,
        contentUri: String = IMAGE_CONTENT_URI,
        width: Int = 640,
        height: Int = 480,
    ): ConversationMessagePartUiModel.Attachment.Image {
        return ConversationMessagePartUiModel.Attachment.Image(
            text = text,
            contentType = contentType,
            contentUri = Uri.parse(contentUri),
            width = width,
            height = height,
        )
    }

    protected fun clickBubble(messageId: String = DEFAULT_MESSAGE_ID) {
        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageBubbleTestTag(messageId = messageId),
            )
            .performClick()
    }

    protected fun longClickBubble(messageId: String = DEFAULT_MESSAGE_ID) {
        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageBubbleTestTag(messageId = messageId),
            )
            .performSemanticsAction(SemanticsActions.OnLongClick)
    }

    protected fun clickSelectionRow(messageId: String = DEFAULT_MESSAGE_ID) {
        composeTestRule
            .onNodeWithTag(
                testTag = conversationMessageSelectionRowTestTag(messageId = messageId),
            )
            .performClick()
    }

    protected fun longClickAvatar() {
        composeTestRule
            .onNodeWithTag(testTag = AVATAR_TAG)
            .performSemanticsAction(SemanticsActions.OnLongClick)
    }

    protected fun clickAvatar() {
        composeTestRule
            .onNodeWithTag(testTag = AVATAR_TAG)
            .performClick()
    }

    protected companion object {
        protected const val AVATAR_TAG = "conversation-message-avatar-under-test"
        protected const val DEFAULT_BODY_TEXT = "Message body"
        protected const val DEFAULT_MESSAGE_ID = "message-1"
        protected const val IMAGE_CONTENT_TYPE = "image/jpeg"
        protected const val IMAGE_CONTENT_URI = "content://mms/part/image-1"
        protected const val MMS_EXPIRY_TIMESTAMP = 1_700_003_600_000L
        protected const val MMS_SIZE_BYTES = 1_572_864L
        protected const val SELF_PARTICIPANT_ID = "self-1"
        protected const val SIM_DISPLAY_NAME = "Work SIM"
        protected const val TIMESTAMP = 1_700_000_000_000L
    }
}
