package com.android.messaging.ui.conversation.metadata.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.ui.conversation.CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.core.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationTopAppBarLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun title_fillsAppBarHeightForTouchTarget() {
        composeTestRule.setContent {
            AppTheme {
                ConversationTopAppBar(
                    metadata = presentMetadata,
                    onAddPeopleClick = {},
                    onTitleClick = {},
                    onNavigateBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag(testTag = CONVERSATION_TOP_APP_BAR_TITLE_TEST_TAG)
            .assertHeightIsEqualTo(expectedHeight = TopAppBarDefaults.TopAppBarExpandedHeight)
    }

    private companion object {
        private val presentMetadata = ConversationMetadataUiState.Present(
            title = "Carol",
            selfParticipantId = "self-participant-id",
            avatar = ConversationMetadataUiState.Avatar.Single(photoUri = null),
            participantCount = 1,
            otherParticipantDisplayDestination = "+372 5440 0024",
            otherParticipantPhoneNumber = "+37254400024",
            otherParticipantContactLookupKey = null,
            isArchived = false,
            composerAvailability = ConversationComposerAvailability.Editable,
        )
    }
}
