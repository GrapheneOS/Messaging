package com.android.messaging.ui.conversation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.android.messaging.R
import com.android.messaging.data.conversation.model.metadata.ConversationComposerDisabledReason
import com.android.messaging.testutil.TEST_CONVERSATION_ID
import com.android.messaging.ui.common.components.LOADING_INDICATOR_DELAY
import com.android.messaging.ui.conversation.CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_READ_ONLY_NOTICE_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.screen.model.ConversationPendingLaunchPayload
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenRenderingTest : BaseConversationScreenTest() {

    @Test
    fun nullConversationId_rendersLoadingState() {
        val screenModel = createScreenModel()

        composeTestRule.setContent {
            AppTheme {
                ConversationScreen(
                    screenModel = screenModel.model,
                    onAddPeopleClick = {},
                    onConversationDetailsClick = {},
                    onNavigateToMessageDetails = {},
                    onNavigateToVCardDetail = {},
                    onNavigateToPhotoViewer = {},
                    onNavigateToAddContact = {},
                    onNavigateToForward = {},
                    onNavigateBack = {},
                    onCloseConversation = {},
                    pendingLaunchPayload = ConversationPendingLaunchPayload(),
                    onPendingDraftConsumed = {},
                    onPendingScrollMessageIdConsumed = {},
                    onPendingSelfParticipantIdConsumed = {},
                    onPendingStartupAttachmentConsumed = {},
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = LOADING_INDICATOR_DELAY.inWholeMilliseconds,
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsLoadingIndicatorAfterDelay() {
        val screenModel = createScreenModel()

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertDoesNotExist()

        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = LOADING_INDICATOR_DELAY.inWholeMilliseconds,
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun presentState_showsMessagesList() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 8,
                latestMessageId = "message-8",
                latestMessageIncoming = false,
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun presentMessagesWithLoadingSimSelector_showsLoadingIndicator() {
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
        )
        screenModel.scaffoldUiStateFlow.value = uiState.copy(
            composer = uiState.composer.copy(
                simSelector = ConversationSimSelectorUiState(
                    isLoading = true,
                ),
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = LOADING_INDICATOR_DELAY.inWholeMilliseconds,
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun readOnlyConversation_showsNoticeInsteadOfComposer() {
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = true,
            ),
        )
        screenModel.scaffoldUiStateFlow.value = uiState.copy(
            composer = uiState.composer.copy(
                disabledReason = ConversationComposerDisabledReason.READ_ONLY_CONVERSATION,
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_READ_ONLY_NOTICE_TEST_TAG)
            .assertIsDisplayed()
            .assertTextEquals(
                composeTestRule.activity.getString(R.string.conversation_cannot_reply),
            )
        composeTestRule
            .onNodeWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun editableConversation_showsComposerWithoutNotice() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = true,
            ),
        )

        setContent(screenModel = screenModel.model)

        composeTestRule
            .onNodeWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_READ_ONLY_NOTICE_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun scaffold_whenMediaPickerOpen_hidesComposerBottomBar() {
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
        )

        setScaffoldContent(
            uiState = { uiState },
            isMediaPickerOpen = true,
        )

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun scaffold_whileAttachmentStripExpands_keepsMessagesFlushWithBottomBar() {
        val uiState = mutableStateOf(value = createScaffoldUiStateWithAttachments())
        setScaffoldContent(uiState = { uiState.value })
        composeTestRule.mainClock.autoAdvance = false

        uiState.value = createScaffoldUiStateWithAttachments(
            attachments = persistentListOf(ATTACHMENT),
        )

        assertMessagesFlushWithBottomBarEachFrame()
    }

    @Test
    fun scaffold_whileAttachmentStripCollapses_keepsMessagesFlushWithBottomBar() {
        val uiState = mutableStateOf(
            value = createScaffoldUiStateWithAttachments(
                attachments = persistentListOf(ATTACHMENT),
            ),
        )
        setScaffoldContent(uiState = { uiState.value })
        composeTestRule.mainClock.autoAdvance = false

        uiState.value = createScaffoldUiStateWithAttachments()

        assertMessagesFlushWithBottomBarEachFrame()
    }

    private fun assertMessagesFlushWithBottomBarEachFrame() {
        repeat(times = RESIZE_FRAME_COUNT) { frame ->
            composeTestRule.mainClock.advanceTimeByFrame()

            assertEquals(
                "Messages bottom vs bottom bar top on frame $frame",
                bottomBarTop(),
                messagesBottom(),
                0f,
            )
        }
    }

    private fun messagesBottom(): Float {
        return composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
    }

    private fun bottomBarTop(): Float {
        val composeBarTop = composeTestRule
            .onNodeWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
            .top
        val attachmentStripHeight = composeTestRule
            .onAllNodesWithTag(CONVERSATION_ATTACHMENT_PREVIEW_LIST_TEST_TAG)
            .fetchSemanticsNodes()
            .singleOrNull()
            ?.boundsInRoot
            ?.height
            ?: 0f

        return composeBarTop - attachmentStripHeight
    }

    private fun createScaffoldUiStateWithAttachments(
        attachments: ImmutableList<ComposerAttachmentUiModel> = persistentListOf(),
    ): ConversationScreenScaffoldUiState {
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
        )

        return uiState.copy(
            composer = uiState.composer.copy(attachments = attachments),
        )
    }

    private fun setScaffoldContent(
        uiState: () -> ConversationScreenScaffoldUiState,
        isMediaPickerOpen: Boolean = false,
    ) {
        val screenModel = createScreenModel()

        composeTestRule.setContent {
            AppTheme {
                ConversationScreenScaffold(
                    conversationId = TEST_CONVERSATION_ID,
                    uiState = uiState(),
                    snackbarHostState = SnackbarHostState(),
                    isMediaPickerOpen = isMediaPickerOpen,
                    messageFieldFocusRequester = FocusRequester(),
                    pendingScrollMessageId = null,
                    onPendingScrollMessageIdConsumed = {},
                    onAddPeopleClick = {},
                    onConversationDetailsClick = {},
                    onNavigateBack = {},
                    onOpenContactPicker = {},
                    onOpenMediaPicker = {},
                    onAudioRecordingStartRequest = {},
                    onLockedAudioRecordingStartRequest = {},
                    screenModel = screenModel.model,
                )
            }
        }
    }

    private companion object {
        private const val RESIZE_FRAME_COUNT = 30

        private val ATTACHMENT = ComposerAttachmentUiModel.Pending.Generic(
            key = "pending-1",
            contentType = "image/jpeg",
            contentUri = "content://media/pending/1",
            displayName = "pending.jpg",
        )
    }
}
