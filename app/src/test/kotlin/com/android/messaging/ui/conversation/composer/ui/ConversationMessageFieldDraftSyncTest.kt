package com.android.messaging.ui.conversation.composer.ui

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID
import com.android.messaging.testutil.focusMessageField
import com.android.messaging.ui.common.components.composer.MESSAGE_COMPOSE_FIELD_TEST_TAG
import com.android.messaging.ui.common.components.composer.MessageComposeBar
import com.android.messaging.ui.conversation.composer.delegate.ConversationDraftEditorDelegateImpl
import com.android.messaging.ui.conversation.composer.delegate.DraftSendRequest
import com.android.messaging.ui.conversation.composer.delegate.PersistedDraftUpdate
import com.android.messaging.ui.conversation.composer.model.ConversationDraftState
import com.android.messaging.ui.core.AppTheme
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The message field against the real draft it edits. The conversation screen learns about draft
 * changes asynchronously, so what it shows can lag behind the draft; [shownState] stands in for
 * that lagging copy.
 */
@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageFieldDraftSyncTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val delegate = ConversationDraftEditorDelegateImpl(
        subscriptionsRepository = mockk(relaxed = true),
        resolveConversationDraftSendProtocol = mockk(relaxed = true),
        resolveDraftAttachmentsWithinLimit = mockk(relaxed = true),
    )
    private var shownState by mutableStateOf(ConversationDraftState())

    @Before
    fun loadDraft() {
        delegate.reset(conversationId = TEST_CONVERSATION_ID)
        delegate.applyPersistedDraftUpdate(
            persistedDraftUpdate = persistedDraftUpdate(messageText = "Hello"),
        )
        shownState = delegate.state.value
    }

    @Test
    fun messageField_showingAnOlderDraftLate_keepsTheNewerDraft() {
        val field = focusedField()

        composeTestRule.runOnIdle {
            delegate.applyPersistedDraftUpdate(
                persistedDraftUpdate = persistedDraftUpdate(messageText = "Saved update"),
            )
            val olderState = delegate.state.value
            delegate.seedDraft(
                conversationId = TEST_CONVERSATION_ID,
                draft = ConversationDraft(messageText = "Shared text"),
            )
            shownState = olderState
        }
        composeTestRule.waitForIdle()
        showLatestState()

        assertEquals("Shared text", delegate.state.value.draft.messageText)
        assertEquals("Shared text", field.text.toString())
    }

    @Test
    fun messageField_typingOverASendClearItHasNotShownYet_endsEmpty() {
        val field = focusedField()

        composeTestRule.runOnIdle {
            delegate.clearConversationDraftAfterSend(
                sendRequest = DraftSendRequest(
                    conversationId = TEST_CONVERSATION_ID,
                    draft = ConversationDraft(messageText = "Hello"),
                ),
            )
            field.inputConnection().commitText("!", 1)
        }
        showLatestState()

        assertEquals("", delegate.state.value.draft.messageText)
        assertEquals("", field.text.toString())
    }

    @Test
    fun messageField_sendClearAfterRetypingTheSameText_endsEmpty() {
        val field = focusedField()

        composeTestRule.runOnIdle {
            val inputConnection = field.inputConnection()
            inputConnection.beginBatchEdit()
            inputConnection.setSelection(0, field.length())
            inputConnection.commitText("", 1)
            inputConnection.commitText("Hello", 1)
            inputConnection.endBatchEdit()
            delegate.clearConversationDraftAfterSend(
                sendRequest = DraftSendRequest(
                    conversationId = TEST_CONVERSATION_ID,
                    draft = ConversationDraft(messageText = "Hello"),
                ),
            )
        }
        showLatestState()

        assertEquals("", delegate.state.value.draft.messageText)
        assertEquals("", field.text.toString())
    }

    private fun showLatestState() {
        composeTestRule.runOnIdle { shownState = delegate.state.value }
        composeTestRule.waitForIdle()
    }

    private fun persistedDraftUpdate(messageText: String): PersistedDraftUpdate {
        return PersistedDraftUpdate(
            conversationId = TEST_CONVERSATION_ID,
            persistedDraft = ConversationDraft(messageText = messageText),
        )
    }

    private fun EditText.inputConnection(): InputConnection {
        return onCreateInputConnection(EditorInfo())!!
    }

    private fun focusedField(): EditText {
        composeTestRule.setContent {
            AppTheme {
                MessageComposeBar(
                    text = shownState.draft.messageText,
                    textRevision = shownState.messageTextRevision,
                    onTextChange = { text, textRevision ->
                        delegate.onMessageTextChanged(
                            messageText = text,
                            messageTextRevision = textRevision,
                        )
                    },
                    isFieldEnabled = true,
                    isFieldContentHidden = false,
                    fieldFocusRequester = null,
                    fieldStateDescription = null,
                    fieldTestTag = MESSAGE_COMPOSE_FIELD_TEST_TAG,
                    sendAction = {},
                )
            }
        }

        return composeTestRule.focusMessageField(testTag = MESSAGE_COMPOSE_FIELD_TEST_TAG)
    }
}
