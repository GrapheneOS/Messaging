package com.android.messaging.data.conversationlist.repository

import android.content.ContentResolver
import android.database.ContentObserver
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.ConversationListItem
import com.android.messaging.data.conversationlist.model.ConversationListMode
import com.android.messaging.data.conversationlist.model.ConversationListSnapshot
import com.android.messaging.data.conversationlist.model.SearchableConversation
import com.android.messaging.data.conversationlist.model.SearchableParticipant
import com.android.messaging.data.conversationlist.store.ConversationListSearchDatabaseStore
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.conversationlist.conversationItem
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationListSearchRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contentResolver = mockk<ContentResolver>()
    private val conversationListRepository = mockk<ConversationListRepository>()
    private val databaseStore = mockk<ConversationListSearchDatabaseStore>()
    private val registeredObserver = slot<ContentObserver>()

    @Before
    fun setUp() {
        every {
            contentResolver.registerContentObserver(any(), true, capture(registeredObserver))
        } just runs
        every { contentResolver.unregisterContentObserver(any()) } just runs
        every { databaseStore.readParticipants() } returns persistentMapOf()
    }

    @Test
    fun observeSearchableConversations_listsInboxThenArchivedWithTheirParticipants() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val inboxItem = conversationItem(conversationId = INBOX_ID)
            val archivedItem = conversationItem(conversationId = ARCHIVED_ID, isArchived = true)
            stubSnapshots(
                inbox = snapshot(inboxItem),
                archived = snapshot(archivedItem),
            )
            every { databaseStore.readParticipants() } returns persistentMapOf(
                INBOX_ID to persistentListOf(ANNA),
            )

            createRepository().observeSearchableConversations().test {
                assertEquals(
                    listOf(
                        SearchableConversation(
                            item = inboxItem,
                            participants = persistentListOf(ANNA),
                        ),
                        SearchableConversation(
                            item = archivedItem,
                            participants = persistentListOf(),
                        ),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeSearchableConversations_leavesOutDirectChatsWithBlockedSenders() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val blockedItem = conversationItem(conversationId = BLOCKED_ID, isArchived = true)
            val blockedGroup = conversationItem(conversationId = GROUP_ID).let { item ->
                item.copy(participant = item.participant.copy(isGroup = true))
            }
            val blockedDestination = blockedItem.participant.otherNormalizedDestination.orEmpty()
            stubSnapshots(
                inbox = snapshot(
                    blockedGroup.copy(
                        participant = blockedGroup.participant.copy(
                            otherNormalizedDestination = blockedDestination,
                        ),
                    ),
                    blockedDestinations = setOf(blockedDestination),
                ),
                archived = snapshot(blockedItem),
            )

            createRepository().observeSearchableConversations().test {
                assertEquals(
                    listOf(GROUP_ID),
                    awaitItem().map { conversation -> conversation.item.conversationId },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeSearchableConversations_conversationsOrParticipantsChange_readsParticipantsAgain() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubSnapshots(
                inbox = snapshot(conversationItem(conversationId = INBOX_ID)),
                archived = snapshot(),
            )

            createRepository().observeSearchableConversations().test {
                awaitItem()

                every { databaseStore.readParticipants() } returns persistentMapOf(
                    INBOX_ID to persistentListOf(ANNA),
                )
                registeredObserver.captured.onChange(false)

                assertEquals(persistentListOf(ANNA), awaitItem().single().participants)
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                contentResolver.registerContentObserver(
                    MessagingContentProvider.CONVERSATIONS_URI,
                    true,
                    any(),
                )
            }
            verify {
                contentResolver.registerContentObserver(
                    MessagingContentProvider.PARTICIPANTS_URI,
                    true,
                    any(),
                )
            }
        }
    }

    private fun stubSnapshots(
        inbox: ConversationListSnapshot,
        archived: ConversationListSnapshot,
    ) {
        every {
            conversationListRepository.observeSnapshot(mode = ConversationListMode.Inbox)
        } returns flowOf(inbox)
        every {
            conversationListRepository.observeSnapshot(mode = ConversationListMode.Archived)
        } returns flowOf(archived)
    }

    private fun snapshot(
        vararg items: ConversationListItem,
        blockedDestinations: Set<String> = emptySet(),
    ): ConversationListSnapshot {
        return ConversationListSnapshot(
            items = items.toList().toImmutableList(),
            blockedDestinations = persistentSetOf<String>().addAll(blockedDestinations),
            hasFirstSyncCompleted = true,
        )
    }

    private fun createRepository(): ConversationListSearchRepositoryImpl {
        return ConversationListSearchRepositoryImpl(
            contentResolver = contentResolver,
            conversationListRepository = conversationListRepository,
            databaseStore = databaseStore,
            messagingDbDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private companion object {
        private val INBOX_ID = ConversationId("1")
        private val ARCHIVED_ID = ConversationId("2")
        private val BLOCKED_ID = ConversationId("3")
        private val GROUP_ID = ConversationId("4")

        private val ANNA = SearchableParticipant(
            fullName = "Anna Smith",
            firstName = "Anna",
            displayDestination = "+15550100",
            normalizedDestination = "+15550100",
        )
    }
}
