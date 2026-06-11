package com.android.messaging.ui.conversation.screen

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMediaPickerOverlayUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule

internal abstract class BaseConversationScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        unmockkAll()
        clearAllMocks()
    }

    protected fun setContent(
        screenModel: ConversationScreenModel,
        conversationId: () -> String? = { CONVERSATION_ID },
        launchGeneration: () -> Int? = { 1 },
        cancelIncomingNotification: Boolean = true,
        lifecycleOwner: LifecycleOwner? = null,
        onAddPeopleClick: () -> Unit = {},
        pendingDraft: ConversationDraft? = null,
        pendingSelfParticipantId: String? = null,
        pendingStartupAttachment: ConversationEntryStartupAttachment? = null,
        onPendingDraftConsumed: () -> Unit = {},
        onPendingSelfParticipantIdConsumed: () -> Unit = {},
        onPendingStartupAttachmentConsumed: () -> Unit = {},
        pendingScrollPosition: Int? = null,
        onPendingScrollPositionConsumed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            val content: @Composable () -> Unit = {
                ConversationScreen(
                    conversationId = conversationId(),
                    launchGeneration = launchGeneration(),
                    cancelIncomingNotification = cancelIncomingNotification,
                    onAddPeopleClick = onAddPeopleClick,
                    onConversationDetailsClick = {},
                    onNavigateBack = {},
                    pendingDraft = pendingDraft,
                    pendingSelfParticipantId = pendingSelfParticipantId,
                    pendingStartupAttachment = pendingStartupAttachment,
                    onPendingDraftConsumed = onPendingDraftConsumed,
                    onPendingSelfParticipantIdConsumed = onPendingSelfParticipantIdConsumed,
                    onPendingStartupAttachmentConsumed = onPendingStartupAttachmentConsumed,
                    pendingScrollPosition = pendingScrollPosition,
                    onPendingScrollPositionConsumed = onPendingScrollPositionConsumed,
                    screenModel = screenModel,
                )
            }
            AppTheme {
                if (lifecycleOwner == null) {
                    content()
                } else {
                    CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                        content()
                    }
                }
            }
        }
    }

    protected fun createPresentUiState(
        messages: List<ConversationMessageUiModel>,
        canAddPeople: Boolean = false,
        canCall: Boolean = false,
        canArchive: Boolean = false,
        canUnarchive: Boolean = false,
        canAddContact: Boolean = false,
        canDeleteConversation: Boolean = false,
        isDeleteConversationConfirmationVisible: Boolean = false,
        otherParticipantPhoneNumber: String? = null,
        otherParticipantContactLookupKey: String? = null,
        isArchived: Boolean = false,
        selection: ConversationMessageSelectionUiState = ConversationMessageSelectionUiState(),
    ): ConversationScreenScaffoldUiState {
        return ConversationScreenScaffoldUiState(
            canAddPeople = canAddPeople,
            canCall = canCall,
            canArchive = canArchive,
            canUnarchive = canUnarchive,
            canAddContact = canAddContact,
            canDeleteConversation = canDeleteConversation,
            isDeleteConversationConfirmationVisible = isDeleteConversationConfirmationVisible,
            metadata = ConversationMetadataUiState.Present(
                title = "Weekend plan",
                selfParticipantId = "self-1",
                avatar = ConversationMetadataUiState.Avatar.Single(
                    photoUri = null,
                ),
                participantCount = 2,
                otherParticipantDisplayDestination = null,
                otherParticipantPhoneNumber = otherParticipantPhoneNumber,
                otherParticipantContactLookupKey = otherParticipantContactLookupKey,
                isArchived = isArchived,
                composerAvailability = ConversationComposerAvailability.Editable,
            ),
            messages = ConversationMessagesUiState.Present(
                messages = messages.toPersistentList(),
            ),
            composer = ConversationComposerUiState(
                isMessageFieldEnabled = true,
                isAttachmentActionEnabled = true,
                isSendEnabled = true,
            ),
            selection = selection,
        )
    }

    protected fun createMessages(
        count: Int,
        latestMessageId: String,
        latestMessageIncoming: Boolean,
        messageIdPrefix: String = "message",
    ): List<ConversationMessageUiModel> {
        val messages = mutableListOf<ConversationMessageUiModel>()
        for (index in 1..count) {
            val messageId = "$messageIdPrefix-$index"
            val isLatestMessage = messageId == latestMessageId
            messages += ConversationMessageUiModel(
                messageId = messageId,
                conversationId = CONVERSATION_ID,
                text = "Message $index",
                parts = persistentListOf(),
                sentTimestamp = index.toLong(),
                receivedTimestamp = index.toLong(),
                displayTimestamp = index.toLong(),
                status = ConversationMessageUiModel.Status.Outgoing.Complete,
                isIncoming = isLatestMessage && latestMessageIncoming,
                senderDisplayName = null,
                senderAvatarUri = null,
                senderContactId = -1L,
                senderContactLookupKey = null,
                senderNormalizedDestination = null,
                senderParticipantId = null,
                selfParticipantId = "self-1",
                canClusterWithPrevious = false,
                canClusterWithNext = false,
                canCopyMessageToClipboard = !latestMessageIncoming,
                canDownloadMessage = false,
                canForwardMessage = true,
                canResendMessage = false,
                canSaveAttachments = false,
                mmsDownload = null,
                mmsSubject = null,
                protocol = ConversationMessageUiModel.Protocol.SMS,
            )
        }

        return messages
    }

    protected fun createScreenModel(): ScreenModelHandle {
        val effectsFlow = MutableSharedFlow<ConversationScreenEffect>()
        val scaffoldUiStateFlow = MutableStateFlow(ConversationScreenScaffoldUiState())
        val mediaPickerOverlayUiStateFlow = MutableStateFlow(
            ConversationMediaPickerOverlayUiState(),
        )
        val model = mockk<ConversationScreenModel>(relaxed = true)

        every { model.effects } returns effectsFlow
        every { model.scaffoldUiState } returns scaffoldUiStateFlow
        every { model.mediaPickerOverlayUiState } returns mediaPickerOverlayUiStateFlow

        return ScreenModelHandle(
            model = model,
            scaffoldUiStateFlow = scaffoldUiStateFlow,
        )
    }

    protected class ScreenModelHandle(
        val model: ConversationScreenModel,
        val scaffoldUiStateFlow: MutableStateFlow<ConversationScreenScaffoldUiState>,
    )
}
