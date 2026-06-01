package com.android.messaging.ui.conversation.screen

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenLaunchPayloadsTest : BaseConversationScreenTest() {

    @Test
    fun pendingLaunchPayloads_seedDraftOpenAttachmentAndNotifyConsumption() {
        val screenModel = createScreenModel()
        var draftConsumedCount = 0
        var attachmentConsumedCount = 0
        val pendingDraft = ConversationDraft(
            messageText = "Hello",
        )
        val pendingAttachment = ConversationEntryStartupAttachment(
            contentType = "image/jpeg",
            contentUri = "content://media/image/1",
        )

        setContent(
            screenModel = screenModel.model,
            pendingDraft = pendingDraft,
            pendingStartupAttachment = pendingAttachment,
            onPendingDraftConsumed = {
                draftConsumedCount += 1
            },
            onPendingStartupAttachmentConsumed = {
                attachmentConsumedCount += 1
            },
        )
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onSeedDraft(
                    conversationId = CONVERSATION_ID,
                    draft = pendingDraft,
                )
            }
            verify(exactly = 1) {
                screenModel.model.onOpenStartupAttachment(
                    conversationId = CONVERSATION_ID,
                    startupAttachment = pendingAttachment,
                )
            }
            assertEquals(1, draftConsumedCount)
            assertEquals(1, attachmentConsumedCount)
        }
    }
}
