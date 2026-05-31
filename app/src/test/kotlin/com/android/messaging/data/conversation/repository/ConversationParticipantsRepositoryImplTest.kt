package com.android.messaging.data.conversation.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.recipient.ConversationRecipient
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TestParticipantRow
import com.android.messaging.testutil.createParticipantsCursor
import com.android.messaging.testutil.participantRow
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationParticipantsRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        contentResolver = mockk()
    }

    @Test
    fun getParticipants_registersAndUnregistersObserverForCollection() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationParticipantsUri(
                CONVERSATION_ID,
            )
            val repository = createRepository()

            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )
            every {
                contentResolver.query(
                    expectedUri,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } returns createParticipantsCursor()

            repository.getParticipants(conversationId = CONVERSATION_ID).test {
                assertEquals(emptyList<ConversationRecipient>(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                contentResolver.registerContentObserver(
                    expectedUri,
                    true,
                    registeredObserver.captured,
                )
            }
            verify(exactly = 1) {
                contentResolver.unregisterContentObserver(registeredObserver.captured)
            }
        }
    }

    @Test
    fun getParticipants_mapsRowsAndFiltersSelfBlankAndDuplicateDestinations() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationParticipantsUri(
                CONVERSATION_ID,
            )
            val repository = createRepository()

            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )
            every {
                contentResolver.query(
                    expectedUri,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } returns createParticipantsCursor(
                recipientParticipantRow(
                    participantId = "self-1",
                    subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                    sendDestination = "+1 555 0000",
                    displayDestination = "+1 555 0000",
                    fullName = "Self",
                ),
                recipientParticipantRow(
                    participantId = "1",
                    sendDestination = "+1 555 0100",
                    displayDestination = "+1 555 0100",
                    fullName = "Ada",
                    profilePhotoUri = "content://photos/1",
                ),
                recipientParticipantRow(
                    participantId = "2",
                    sendDestination = "   ",
                    displayDestination = "   ",
                    fullName = "Ignored",
                ),
                recipientParticipantRow(
                    participantId = "3",
                    sendDestination = "+1 555 0100",
                    displayDestination = "+1 555 0100",
                    fullName = "Ada Duplicate",
                ),
                recipientParticipantRow(
                    participantId = "4",
                    sendDestination = "+1 555 0101",
                    displayDestination = "Bob",
                    fullName = "Bob",
                ),
            )

            repository.getParticipants(conversationId = CONVERSATION_ID).test {
                assertEquals(
                    listOf(
                        ConversationRecipient(
                            id = "1",
                            displayName = "Ada",
                            destination = "+1 555 0100",
                            photoUri = "content://photos/1",
                            secondaryText = "+1 555 0100",
                        ),
                        ConversationRecipient(
                            id = "4",
                            displayName = "Bob",
                            destination = "+1 555 0101",
                            secondaryText = null,
                        ),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun getParticipants_requeriesWhenConversationParticipantsChange() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.buildConversationParticipantsUri(
                CONVERSATION_ID,
            )
            val repository = createRepository()
            var currentCursor = createParticipantsCursor(
                recipientParticipantRow(
                    participantId = "1",
                    sendDestination = "+1 555 0100",
                    displayDestination = "+1 555 0100",
                    fullName = "Ada",
                ),
            )

            stubObserverRegistration(
                expectedUri = expectedUri,
                registeredObserver = registeredObserver,
            )
            every {
                contentResolver.query(
                    expectedUri,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    null,
                    null,
                    null,
                )
            } answers {
                currentCursor
            }

            repository.getParticipants(conversationId = CONVERSATION_ID).test {
                assertEquals(
                    listOf(
                        ConversationRecipient(
                            id = "1",
                            displayName = "Ada",
                            destination = "+1 555 0100",
                            secondaryText = "+1 555 0100",
                        ),
                    ),
                    awaitItem(),
                )

                currentCursor = createParticipantsCursor(
                    recipientParticipantRow(
                        participantId = "2",
                        sendDestination = "+1 555 0101",
                        displayDestination = "+1 555 0101",
                        fullName = "Bob",
                    ),
                )
                registeredObserver.captured.onChange(false)

                assertEquals(
                    listOf(
                        ConversationRecipient(
                            id = "2",
                            displayName = "Bob",
                            destination = "+1 555 0101",
                            secondaryText = "+1 555 0101",
                        ),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun createRepository(): ConversationParticipantsRepositoryImpl {
        return ConversationParticipantsRepositoryImpl(
            contentResolver = contentResolver,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            messagingDbDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun stubObserverRegistration(
        expectedUri: Uri,
        registeredObserver: CapturingSlot<ContentObserver>,
    ) {
        every {
            contentResolver.registerContentObserver(
                expectedUri,
                true,
                capture(registeredObserver),
            )
        } just runs
        every { contentResolver.unregisterContentObserver(any()) } just runs
    }

    private fun recipientParticipantRow(
        participantId: String,
        sendDestination: String,
        displayDestination: String,
        fullName: String,
        subId: Int = ParticipantData.OTHER_THAN_SELF_SUB_ID,
        profilePhotoUri: String? = null,
    ): TestParticipantRow {
        return participantRow(
            participantId = participantId,
            subId = subId,
            slotId = ParticipantData.INVALID_SLOT_ID,
            normalizedDestination = sendDestination.trim(),
            sendDestination = sendDestination,
            displayDestination = displayDestination,
            fullName = fullName,
            profilePhotoUri = profilePhotoUri,
            contactId = 1L,
            lookupKey = "lookup-$participantId",
            contactDestination = sendDestination,
        )
    }
}
