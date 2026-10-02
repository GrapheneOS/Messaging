package com.android.messaging.datamodel.action

import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.sms.MmsUtils
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.widget.WidgetConversationProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeleteConversationActionTest {

    private val actionService = mockk<ActionService>()
    private val startedAction = slot<Action>()

    @Before
    fun setUp() {
        val dataModel = mockk<DataModel>(relaxed = true)
        every { dataModel.actionService } returns actionService
        every { actionService.startAction(capture(startedAction)) } just runs
        installTestFactory(
            context = RuntimeEnvironment.getApplication().applicationContext,
            dataModel = dataModel,
        )
        mockkObject(NotificationChannelUtil)
        every { NotificationChannelUtil.deleteChannel(any()) } just runs
        mockkStatic(BugleDatabaseOperations::class)
        every { BugleDatabaseOperations.getThreadId(any(), CONVERSATION_ID) } returns THREAD_ID
        every { BugleDatabaseOperations.deleteConversation(any(), CONVERSATION_ID, any()) } returns
            true
        mockkStatic(MessagingContentProvider::class)
        every { MessagingContentProvider.notifyConversationListChanged() } just runs
        mockkStatic(WidgetConversationProvider::class)
        every { WidgetConversationProvider.notifyConversationDeleted(any(), any()) } just runs
        mockkStatic(BugleNotifications::class)
        every { BugleNotifications.update(any(), any()) } just runs
        mockkStatic(BugleActionToasts::class)
        every { BugleActionToasts.onConversationsDeleted(any()) } just runs
        every { BugleActionToasts.onFailedToDeleteConversations(any()) } just runs
        mockkStatic(MmsUtils::class)
        every { MmsUtils.deleteThread(THREAD_ID, any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun draftOnlyConversationWithNoTelephonyMessagesReportsDeleted() {
        every { MmsUtils.hasThreadMessages(THREAD_ID, any()) } returns false

        deleteConversation()

        verify { BugleActionToasts.onConversationsDeleted(1) }
        verify(exactly = 0) { BugleActionToasts.onFailedToDeleteConversations(any()) }
    }

    @Test
    fun telephonyMessagesThatSurviveTheDeleteReportFailure() {
        every { MmsUtils.hasThreadMessages(THREAD_ID, any()) } returns true

        deleteConversation()

        verify { BugleActionToasts.onFailedToDeleteConversations(1) }
        verify(exactly = 0) { BugleActionToasts.onConversationsDeleted(any()) }
    }

    private fun deleteConversation() {
        DeleteConversationAction.deleteConversation(CONVERSATION_ID, Long.MAX_VALUE)
        startedAction.captured.doBackgroundWork()
    }

    private companion object {
        private const val CONVERSATION_ID = "17"
        private const val THREAD_ID = 42L
    }
}
