package com.android.messaging.ui.conversation.entry

import android.content.Intent
import app.cash.turbine.test
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_CONVERSATION_ID_VALUE as CONVERSATION_ID_VALUE
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversation.entry.model.ConversationEntryLaunchRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConversationLaunchStoreTest {

    private val store = ConversationLaunchStoreImpl()

    @Test
    fun submit_emitsEveryRequestInOrder() = runTest {
        val first = ConversationEntryLaunchRequest(conversationId = CONVERSATION_ID)
        val second = ConversationEntryLaunchRequest(
            conversationId = null,
            startupAttachmentUri = ATTACHMENT_URI,
        )

        store.requests.test {
            store.submit(request = first)
            store.submit(request = second)

            assertThat(awaitItem()).isEqualTo(first)
            assertThat(awaitItem()).isEqualTo(second)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun launch_emitsRequestCarryingTheDraft() = runTest {
        val draft = MessageData()

        store.requests.test {
            store.launch(
                conversationId = CONVERSATION_ID,
                draft = draft,
            )

            val request = awaitItem()

            assertThat(request.conversationId).isEqualTo(CONVERSATION_ID)
            assertThat(request.draftData).isEqualTo(draft)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun launch_withoutDraft_emitsRequestWithoutDraft() = runTest {
        store.requests.test {
            store.launch(
                conversationId = CONVERSATION_ID,
                draft = null,
            )

            assertNull(awaitItem().draftData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun submitIntent_withLaunchPayload_emitsMappedRequest() = runTest {
        val intent = Intent()
            .putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, CONVERSATION_ID_VALUE)
            .putExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA, MessageData())

        store.requests.test {
            store.submitIntent(intent = intent)

            val request = awaitItem()

            assertThat(request.conversationId).isEqualTo(CONVERSATION_ID)
            assertNotNull(request.draftData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun submitIntent_withoutLaunchPayload_emitsNothing() = runTest {
        store.requests.test {
            store.submitIntent(intent = Intent())

            expectNoEvents()
        }
    }

    @Test
    fun submitIntent_repeatedForTheSameIntent_doesNotReapplyTheConsumedDraft() = runTest {
        val intent = Intent()
            .putExtra(UIIntents.UI_INTENT_EXTRA_CONVERSATION_ID, CONVERSATION_ID_VALUE)
            .putExtra(UIIntents.UI_INTENT_EXTRA_DRAFT_DATA, MessageData())
            .putExtra(UIIntents.UI_INTENT_EXTRA_MESSAGE_POSITION, 4)

        store.requests.test {
            store.submitIntent(intent = intent)
            store.submitIntent(intent = intent)

            assertNotNull(awaitItem().draftData)

            val replayed = awaitItem()

            assertThat(replayed.conversationId).isEqualTo(CONVERSATION_ID)
            assertNull(replayed.draftData)
            assertNull(replayed.messagePosition)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val ATTACHMENT_URI = "content://media/external/images/media/1"
    }
}
