package com.android.messaging.ui.conversation.metadata.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test

internal class ConversationTopAppBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun titleTouchTargetUsesSmallTopAppBarHeight() {
        composeRule.setContent {
            AppTheme {
                ConversationTopAppBar(
                    metadata = conversationMetadata(),
                    isCallVisible = true,
                    onAddPeopleClick = {},
                    onTitleClick = {},
                    onNavigateBack = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(testTag = CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG)
            .assertHeightIsEqualTo(expectedHeight = TopAppBarDefaults.TopAppBarExpandedHeight)
    }
}

private fun conversationMetadata(): ConversationMetadataUiState {
    return ConversationMetadataUiState.Present(
        title = "+372 5440 0024",
        selfParticipantId = "self-participant-id",
        avatar = ConversationMetadataUiState.Avatar.Single(
            photoUri = null,
            normalizedDestination = null,
            displayName = null,
        ),
        participantCount = 1,
        otherParticipantDisplayDestination = "+372 5440 0024",
        otherParticipantPhoneNumber = "+37254400024",
        otherParticipantContactLookupKey = null,
        isArchived = false,
        isBlocked = false,
        composerAvailability = ConversationComposerAvailability.Editable,
    )
}
