package com.android.messaging.data.conversationlist.repository

import android.content.ContentResolver
import android.database.MatrixCursor
import app.cash.turbine.test
import com.android.messaging.data.conversationlist.model.ConversationListMode
import com.android.messaging.data.conversationlist.store.ConversationListStatusStore
import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepository
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ConversationListData
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contentResolver = mockk<ContentResolver>()
    private val notificationRepository = mockk<ConversationNotificationRepository>()
    private val statusStore = mockk<ConversationListStatusStore>()

    @Before
    fun setUp() {
        every { contentResolver.registerContentObserver(any(), true, any()) } just runs
        every { contentResolver.unregisterContentObserver(any()) } just runs
        every {
            contentResolver.query(
                MessagingContentProvider.CONVERSATIONS_URI,
                any(),
                any(),
                any(),
                any(),
            )
        } answers { MatrixCursor(emptyArray()) }
        every {
            contentResolver.query(
                MessagingContentProvider.PARTICIPANTS_URI,
                any(),
                any(),
                any(),
                any(),
            )
        } answers {
            MatrixCursor(arrayOf(ParticipantColumns.NORMALIZED_DESTINATION)).apply {
                addRow(arrayOf(BLOCKED_DESTINATION))
            }
        }
        every { notificationRepository.observeSnoozeChanges() } returns emptyFlow()
        every { statusStore.hasFirstSyncCompleted() } returns true
    }

    @Test
    fun observeSnapshot_allMode_readsEveryConversationInOneQuery() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository().observeSnapshot(mode = ConversationListMode.All).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                contentResolver.query(
                    MessagingContentProvider.CONVERSATIONS_URI,
                    any(),
                    isNull(),
                    isNull(),
                    any(),
                )
            }
        }
    }

    @Test
    fun observeSnapshot_allMode_readsTheBlockedSenders() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository().observeSnapshot(mode = ConversationListMode.All).test {
                assertEquals(setOf(BLOCKED_DESTINATION), awaitItem().blockedDestinations)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeSnapshot_inboxMode_readsOnlyConversationsOutsideTheArchive() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository().observeSnapshot(mode = ConversationListMode.Inbox).test {
                assertEquals(setOf(BLOCKED_DESTINATION), awaitItem().blockedDestinations)
                cancelAndIgnoreRemainingEvents()
            }

            verifyConversationsQueried(selection = ConversationListData.WHERE_NOT_ARCHIVED)
        }
    }

    @Test
    fun observeSnapshot_archivedMode_readsOnlyArchivedConversationsAndNoBlockedSenders() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            createRepository().observeSnapshot(mode = ConversationListMode.Archived).test {
                assertEquals(emptySet<String>(), awaitItem().blockedDestinations)
                cancelAndIgnoreRemainingEvents()
            }

            verifyConversationsQueried(selection = ConversationListData.WHERE_ARCHIVED)
        }
    }

    private fun verifyConversationsQueried(selection: String) {
        verify(exactly = 1) {
            contentResolver.query(
                MessagingContentProvider.CONVERSATIONS_URI,
                any(),
                selection,
                isNull(),
                any(),
            )
        }
    }

    private fun createRepository(): ConversationListRepositoryImpl {
        return ConversationListRepositoryImpl(
            contentResolver = contentResolver,
            statusStore = statusStore,
            notificationRepository = notificationRepository,
            messagingDbDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private companion object {
        private const val BLOCKED_DESTINATION = "+15550100"
    }
}
