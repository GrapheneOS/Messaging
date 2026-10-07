package com.android.messaging.data.conversationlist.store

import com.android.messaging.data.secondaryuser.SecondaryUserNotifier
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.SyncManager
import com.android.messaging.datamodel.action.MarkAsSeenAction
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
    private val secondaryUserNotifier = mockk<SecondaryUserNotifier>(relaxed = true)

    private val store = ConversationListStatusStoreImpl(secondaryUserNotifier)

    @Before
    fun setUp() {
        mockkStatic(DataModel::class)
        mockkStatic(MarkAsSeenAction::class)

        every { DataModel.get() } returns dataModel
        every { dataModel.syncManager } returns syncManager
        every { MarkAsSeenAction.markAllAsSeen(any()) } just runs
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
        verify(exactly = 1) { MarkAsSeenAction.markAllAsSeen(false) }
        verify { secondaryUserNotifier.cancel() }
    }

    @Test
    fun setNewestConversationVisible_notVisible_updatesStatusWithoutCancellingNotification() {
        store.setNewestConversationVisible(isVisible = false)

        verify(exactly = 1) {
            dataModel.isConversationListScrolledToNewestConversation = false
        }
        verify(exactly = 0) {
            MarkAsSeenAction.markAllAsSeen(any())
        }
        verify(exactly = 0) {
            secondaryUserNotifier.cancel()
        }
    }
}
