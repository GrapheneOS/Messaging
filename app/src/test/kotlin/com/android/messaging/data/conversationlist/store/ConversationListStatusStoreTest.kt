package com.android.messaging.data.conversationlist.store

import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.SyncManager
import com.android.messaging.receiver.SmsReceiver
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class ConversationListStatusStoreTest {

    private val dataModel = mockk<DataModel>(relaxed = true)
    private val syncManager = mockk<SyncManager>()

    private val store = ConversationListStatusStoreImpl()

    @Before
    fun setUp() {
        mockkStatic(DataModel::class)
        mockkStatic(SmsReceiver::class)

        every { DataModel.get() } returns dataModel
        every { dataModel.syncManager } returns syncManager
        every { SmsReceiver.cancelSecondaryUserNotification() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun hasFirstSyncCompleted_returnsSyncManagerValue() {
        every { syncManager.hasFirstSyncCompleted } returns true

        assertTrue(store.hasFirstSyncCompleted())
    }

    @Test
    fun setNewestConversationVisible_visible_updatesStatusAndCancelsSecondaryNotification() {
        store.setNewestConversationVisible(isVisible = true)

        verify { dataModel.isConversationListScrolledToNewestConversation = true }
        verify { SmsReceiver.cancelSecondaryUserNotification() }
    }

    @Test
    fun setNewestConversationVisible_notVisible_updatesStatusWithoutCancellingNotification() {
        store.setNewestConversationVisible(isVisible = false)

        verify(exactly = 1) {
            dataModel.isConversationListScrolledToNewestConversation = false
        }
        verify(exactly = 0) {
            SmsReceiver.cancelSecondaryUserNotification()
        }
    }
}
