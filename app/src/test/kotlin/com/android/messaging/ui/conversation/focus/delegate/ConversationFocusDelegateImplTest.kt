package com.android.messaging.ui.conversation.focus.delegate

import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationFocusDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataModel: DataModel

    @Before
    fun setUp() {
        dataModel = mockk(relaxed = true)
        mockkStatic(DataModel::class)
        every { DataModel.get() } returns dataModel
        mockkStatic(BugleNotifications::class)
        every {
            BugleNotifications.markMessagesAsRead(any(), any())
        } just runs
    }

    @After
    fun tearDown() {
        unmockkStatic(DataModel::class)
        unmockkStatic(BugleNotifications::class)
    }

    @Test
    fun setScreenFocused_withConversationId_setsFocusAndMarksRead() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val conversationIdFlow = MutableStateFlow<String?>(value = CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)

            delegate.setScreenFocused(focused = true)
            runCurrent()

            verify(exactly = 1) {
                dataModel.setFocusedConversation(CONVERSATION_ID)
            }
            verify(exactly = 1) {
                BugleNotifications.markMessagesAsRead(
                    CONVERSATION_ID,
                    true,
                )
            }
        }
    }

    @Test
    fun setScreenFocused_withCancelNotificationFalse_propagatesFlag() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val conversationIdFlow = MutableStateFlow<String?>(value = CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)

            delegate.setScreenFocused(focused = true, cancelNotification = false)
            runCurrent()

            verify(exactly = 1) {
                BugleNotifications.markMessagesAsRead(
                    CONVERSATION_ID,
                    false,
                )
            }
        }
    }

    @Test
    fun setScreenFocused_withNullConversationId_doesNotMarkRead() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val conversationIdFlow = MutableStateFlow<String?>(value = null)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)

            delegate.setScreenFocused(focused = true)
            runCurrent()

            verify(exactly = 0) {
                BugleNotifications.markMessagesAsRead(any(), any())
            }
            verify(exactly = 0) {
                dataModel.setFocusedConversation(ofType<String>())
            }
        }
    }

    @Test
    fun setScreenFocused_withBlankConversationId_doesNotMarkRead() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val conversationIdFlow = MutableStateFlow<String?>(value = "   ")
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)

            delegate.setScreenFocused(focused = true)
            runCurrent()

            verify(exactly = 0) {
                BugleNotifications.markMessagesAsRead(any(), any())
            }
        }
    }

    @Test
    fun setScreenFocused_unfocused_clearsFocusedConversation() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val focusedConversationIds = mutableListOf<String?>()
            every {
                dataModel.setFocusedConversation(any<String>())
            } answers {
                focusedConversationIds.add(firstArg())
            }
            every {
                dataModel.setFocusedConversation(null)
            } answers {
                focusedConversationIds.add(null)
            }
            val conversationIdFlow = MutableStateFlow<String?>(value = CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)

            delegate.setScreenFocused(focused = true)
            runCurrent()
            delegate.setScreenFocused(focused = false)
            runCurrent()

            assertEquals(listOf(null, CONVERSATION_ID, null), focusedConversationIds)
        }
    }

    @Test
    fun conversationIdSwap_whileFocused_marksReadForNewConversation() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val conversationIdFlow = MutableStateFlow<String?>(value = CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)

            delegate.setScreenFocused(focused = true)
            runCurrent()
            conversationIdFlow.value = "conversation-2"
            runCurrent()

            verify(exactly = 1) {
                BugleNotifications.markMessagesAsRead(CONVERSATION_ID, true)
            }
            verify(exactly = 1) {
                BugleNotifications.markMessagesAsRead("conversation-2", true)
            }
            verify(exactly = 1) {
                dataModel.setFocusedConversation("conversation-2")
            }
        }
    }

    @Test
    fun setScreenFocused_repeatedIdenticalFocusedRequests_marksReadOnce() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val conversationIdFlow = MutableStateFlow<String?>(value = CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)

            delegate.setScreenFocused(focused = true)
            delegate.setScreenFocused(focused = true)
            delegate.setScreenFocused(focused = true)
            runCurrent()

            verify(exactly = 1) {
                BugleNotifications.markMessagesAsRead(CONVERSATION_ID, true)
            }
        }
    }

    @Test
    fun bind_calledTwice_onlyBindsFirstScope() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val conversationIdFlow = MutableStateFlow<String?>(value = CONVERSATION_ID)
            val secondConversationIdFlow = MutableStateFlow<String?>(value = "conversation-rebound")
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            delegate.bind(scope = backgroundScope, conversationIdFlow = secondConversationIdFlow)

            delegate.setScreenFocused(focused = true)
            runCurrent()

            verify(exactly = 1) {
                BugleNotifications.markMessagesAsRead(CONVERSATION_ID, true)
            }
            verify(exactly = 0) {
                BugleNotifications.markMessagesAsRead("conversation-rebound", any())
            }
        }
    }

    private fun TestScope.createBoundDelegate(
        conversationIdFlow: MutableStateFlow<String?>,
    ): ConversationFocusDelegateImpl {
        val delegate = ConversationFocusDelegateImpl(
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
        delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
        runCurrent()
        return delegate
    }
}
