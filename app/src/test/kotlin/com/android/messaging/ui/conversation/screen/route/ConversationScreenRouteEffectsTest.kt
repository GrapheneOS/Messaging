package com.android.messaging.ui.conversation.screen.route

import androidx.lifecycle.Lifecycle
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TestLifecycleOwner
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.entry.model.ConversationEntryStartupAttachment
import com.android.messaging.ui.conversation.screen.BaseConversationScreenTest
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import io.mockk.verify
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenRouteEffectsTest : BaseConversationScreenTest() {

    @Test
    fun pendingPayloads_withoutConversationIdAreIgnored() {
        val screenModel = createScreenModel()
        var draftConsumedCount = 0
        var selfParticipantConsumedCount = 0
        var attachmentConsumedCount = 0
        val pendingDraft = ConversationDraft(
            messageText = "Pending",
        )
        val pendingAttachment = ConversationEntryStartupAttachment(
            contentType = "image/png",
            contentUri = "content://media/image/10",
        )

        setContent(
            screenModel = screenModel.model,
            conversationId = { null },
            launchGeneration = { 3 },
            pendingDraft = pendingDraft,
            pendingSelfParticipantId = SELF_PARTICIPANT_ID,
            pendingStartupAttachment = pendingAttachment,
            onPendingDraftConsumed = {
                draftConsumedCount += 1
            },
            onPendingSelfParticipantIdConsumed = {
                selfParticipantConsumedCount += 1
            },
            onPendingStartupAttachmentConsumed = {
                attachmentConsumedCount += 1
            },
        )
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onConversationIdChanged(conversationId = null)
            }
            verify(exactly = 0) {
                screenModel.model.onSeedDraft(any(), any())
            }
            verify(exactly = 0) {
                screenModel.model.onSimSelected(any())
            }
            verify(exactly = 0) {
                screenModel.model.onOpenStartupAttachment(any(), any())
            }
            assertEquals(0, draftConsumedCount)
            assertEquals(0, selfParticipantConsumedCount)
            assertEquals(0, attachmentConsumedCount)
        }
    }

    @Test
    fun pendingPayloads_withoutLaunchGenerationAreIgnored() {
        val screenModel = createScreenModel()
        var draftConsumedCount = 0
        var selfParticipantConsumedCount = 0
        var attachmentConsumedCount = 0
        val pendingDraft = ConversationDraft(
            messageText = "Pending",
        )
        val pendingAttachment = ConversationEntryStartupAttachment(
            contentType = "image/png",
            contentUri = "content://media/image/10",
        )

        setContent(
            screenModel = screenModel.model,
            launchGeneration = { null },
            pendingDraft = pendingDraft,
            pendingSelfParticipantId = SELF_PARTICIPANT_ID,
            pendingStartupAttachment = pendingAttachment,
            onPendingDraftConsumed = {
                draftConsumedCount += 1
            },
            onPendingSelfParticipantIdConsumed = {
                selfParticipantConsumedCount += 1
            },
            onPendingStartupAttachmentConsumed = {
                attachmentConsumedCount += 1
            },
        )
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                screenModel.model.onSeedDraft(any(), any())
            }
            verify(exactly = 0) {
                screenModel.model.onSimSelected(any())
            }
            verify(exactly = 0) {
                screenModel.model.onOpenStartupAttachment(any(), any())
            }
            assertEquals(0, draftConsumedCount)
            assertEquals(0, selfParticipantConsumedCount)
            assertEquals(0, attachmentConsumedCount)
        }
    }

    @Test
    fun pendingDraft_withConversationAndLaunchGenerationSeedsDraftAndNotifiesConsumption() {
        val screenModel = createScreenModel()
        var draftConsumedCount = 0
        val pendingDraft = ConversationDraft(
            messageText = "Pending body",
            subjectText = "Subject",
        )

        setContent(
            screenModel = screenModel.model,
            pendingDraft = pendingDraft,
            onPendingDraftConsumed = {
                draftConsumedCount += 1
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
            assertEquals(1, draftConsumedCount)
        }
    }

    @Test
    fun pendingSelfParticipantId_selectsSimAndNotifiesConsumption() {
        val screenModel = createScreenModel()
        var selfParticipantConsumedCount = 0

        setContent(
            screenModel = screenModel.model,
            pendingSelfParticipantId = SELF_PARTICIPANT_ID,
            onPendingSelfParticipantIdConsumed = {
                selfParticipantConsumedCount += 1
            },
        )
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onSimSelected(selfParticipantId = SELF_PARTICIPANT_ID)
            }
            assertEquals(1, selfParticipantConsumedCount)
        }
    }

    @Test
    fun pendingStartupAttachment_withConversationAndLaunchGenerationOpensAndNotifiesConsumption() {
        val screenModel = createScreenModel()
        var attachmentConsumedCount = 0
        val pendingAttachment = ConversationEntryStartupAttachment(
            contentType = "image/jpeg",
            contentUri = "content://media/image/22",
        )

        setContent(
            screenModel = screenModel.model,
            pendingStartupAttachment = pendingAttachment,
            onPendingStartupAttachmentConsumed = {
                attachmentConsumedCount += 1
            },
        )
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onOpenStartupAttachment(
                    conversationId = CONVERSATION_ID,
                    startupAttachment = pendingAttachment,
                )
            }
            assertEquals(1, attachmentConsumedCount)
        }
    }

    @Test
    fun foregroundAndBackgroundLifecycleEventsForwardToScreenModel() {
        val screenModel = createScreenModel()
        lateinit var lifecycleOwner: TestLifecycleOwner

        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(
                initialState = Lifecycle.State.CREATED,
            )
        }

        setContent(
            screenModel = screenModel.model,
            lifecycleOwner = lifecycleOwner,
            cancelIncomingNotification = false,
        )
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.RESUMED)
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.STARTED)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onScreenForegrounded(cancelNotification = false)
            }
            verify(exactly = 1) {
                screenModel.model.onScreenBackgrounded()
            }
        }
    }

    @Test
    fun stoppingWithoutRecordingPersistsDraftWithoutCancellingRecording() {
        val screenModel = createScreenModel()
        lateinit var lifecycleOwner: TestLifecycleOwner

        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(
                initialState = Lifecycle.State.RESUMED,
            )
        }

        setContent(
            screenModel = screenModel.model,
            lifecycleOwner = lifecycleOwner,
        )
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.CREATED)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                screenModel.model.onAudioRecordingCancel()
            }
            verify(exactly = 1) {
                screenModel.model.persistDraft()
            }
        }
    }

    @Test
    fun stoppingWhileRecordingCancelsRecordingAndPersistsDraft() {
        val screenModel = createScreenModel()
        lateinit var lifecycleOwner: TestLifecycleOwner
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = false,
            ),
        ).let { uiState ->
            uiState.copy(
                composer = uiState.composer.copy(
                    audioRecording = ConversationAudioRecordingUiState(
                        phase = ConversationAudioRecordingPhase.Recording,
                    ),
                ),
            )
        }

        composeTestRule.runOnIdle {
            lifecycleOwner = TestLifecycleOwner(
                initialState = Lifecycle.State.RESUMED,
            )
        }

        setContent(
            screenModel = screenModel.model,
            lifecycleOwner = lifecycleOwner,
        )
        composeTestRule.runOnIdle {
            lifecycleOwner.moveTo(state = Lifecycle.State.CREATED)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.onAudioRecordingCancel()
            }
            verify(exactly = 1) {
                screenModel.model.persistDraft()
            }
        }
    }

    @Test
    fun backPressInSelectionModeDismissesMessageSelection() {
        val screenModel = createScreenModel()
        screenModel.scaffoldUiStateFlow.value = createPresentUiState(
            messages = createMessages(
                count = 1,
                latestMessageId = "message-1",
                latestMessageIncoming = false,
            ),
            selection = ConversationMessageSelectionUiState(
                selectedMessageIds = persistentSetOf("message-1"),
            ),
        )

        setContent(screenModel = screenModel.model)
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.model.dismissMessageSelection()
            }
        }
    }

    private companion object {
        private const val SELF_PARTICIPANT_ID = "self-2"
    }
}
