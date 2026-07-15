package com.android.messaging.ui.conversation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.android.messaging.testutil.TEST_CONVERSATION_ID
import com.android.messaging.ui.conversation.CONVERSATION_COMPOSE_BAR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_LOADING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.CONVERSATION_MESSAGES_LIST_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.core.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenRenderingTest : BaseConversationScreenTest() {

    @Test
    fun defaultOptionalParameters_renderLoadingState() {
        val screenModel = createScreenModel()

        composeTestRule.setContent {
            AppTheme {
                ConversationScreen(
                    onAddPeopleClick = {},
                    onConversationDetailsClick = {},
                    onNavigateToMessageDetails = {},
                    onNavigateBack = {},
                    screenModel = screenModel.model,
                )
            }
        }

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        val screenModel = createScreenModel()

        setContent(screenModel = screenModel.model)

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

        composeTestRule
            .onNodeWithTag(CONVERSATION_LOADING_INDICATOR_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun scaffold_whenMediaPickerOpen_hidesComposerBottomBar() {
        val screenModel = createScreenModel()
        val uiState = createPresentUiState(
            messages = createMessages(
                count = 2,
                latestMessageId = "message-2",
                latestMessageIncoming = false,
            ),
        )

        composeTestRule.setContent {
            AppTheme {
                ConversationScreenScaffold(
                    conversationId = TEST_CONVERSATION_ID,
                    uiState = uiState,
                    snackbarHostState = SnackbarHostState(),
                    isMediaPickerOpen = true,
                    messageFieldFocusRequester = FocusRequester(),
                    pendingScrollPosition = null,
                    onPendingScrollPositionConsumed = {},
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

        composeTestRule
            .onNodeWithTag(CONVERSATION_MESSAGES_LIST_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(CONVERSATION_COMPOSE_BAR_TEST_TAG)
            .assertDoesNotExist()
    }
}
