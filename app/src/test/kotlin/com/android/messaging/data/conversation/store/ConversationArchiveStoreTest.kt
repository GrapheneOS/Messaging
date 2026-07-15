package com.android.messaging.data.conversation.store

import com.android.messaging.datamodel.BugleDatabaseOperations
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MessagingContentProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationArchiveStoreTest {

    private val databaseWrapper = mockk<DatabaseWrapper>(relaxed = true)
    private val dataModel = mockk<DataModel>()

    private val store = ConversationArchiveStoreImpl()

    @Before
    fun setUp() {
        mockkStatic(DataModel::class)
        mockkStatic(BugleDatabaseOperations::class)
        mockkStatic(MessagingContentProvider::class)

        every { DataModel.get() } returns dataModel
        every { dataModel.database } returns databaseWrapper
        every {
            BugleDatabaseOperations.updateConversationArchiveStatusInTransaction(
                any(),
                any(),
                any(),
            )
        } just runs
        every { MessagingContentProvider.notifyConversationListChanged() } just runs
        every { MessagingContentProvider.notifyConversationMetadataChanged(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun archiveConversation_updatesArchiveStatusToTrueInsideTransactionAndNotifies() {
        store.archiveConversation(CONVERSATION_ID)

        verifyOrder {
            databaseWrapper.beginTransaction()
            BugleDatabaseOperations.updateConversationArchiveStatusInTransaction(
                databaseWrapper,
                CONVERSATION_ID,
                true,
            )
            databaseWrapper.setTransactionSuccessful()
            databaseWrapper.endTransaction()
        }
        verify(exactly = 1) {
            MessagingContentProvider.notifyConversationListChanged()
        }
        verify(exactly = 1) {
            MessagingContentProvider.notifyConversationMetadataChanged(CONVERSATION_ID)
        }
    }

    @Test
    fun unarchiveConversation_updatesArchiveStatusToFalse() {
        store.unarchiveConversation(conversationId = CONVERSATION_ID)

        verify(exactly = 1) {
            BugleDatabaseOperations.updateConversationArchiveStatusInTransaction(
                databaseWrapper,
                CONVERSATION_ID,
                false,
            )
        }
    }

    @Test
    fun archiveConversation_updateFails_endsTransactionWithoutNotifying() {
        val failure = IllegalStateException("update failed")
        every {
            BugleDatabaseOperations.updateConversationArchiveStatusInTransaction(
                any(),
                any(),
                any(),
            )
        } throws failure

        val thrown = assertThrows(IllegalStateException::class.java) {
            store.archiveConversation(CONVERSATION_ID)
        }

        assertSame(failure, thrown)
        verify(exactly = 1) { databaseWrapper.endTransaction() }
        verify(exactly = 0) { databaseWrapper.setTransactionSuccessful() }
        verify(exactly = 0) { MessagingContentProvider.notifyConversationListChanged() }
        verify(exactly = 0) {
            MessagingContentProvider.notifyConversationMetadataChanged(any())
        }
    }

    private companion object {
        private const val CONVERSATION_ID = "conversation-42"
    }
}
