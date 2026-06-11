package com.android.messaging.data.subscription.repository

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.database.MatrixCursor
import android.net.Uri
import android.telephony.SubscriptionManager
import app.cash.turbine.test
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.debug.DebugSimEmulationMode
import com.android.messaging.debug.DebugSimEmulationSource
import com.android.messaging.sms.MmsConfig
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.createParticipantsCursor
import com.android.messaging.testutil.participantRow
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SubscriptionsRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var contentResolver: ContentResolver
    private lateinit var emulationModeFlow: MutableStateFlow<DebugSimEmulationMode>
    private lateinit var emulationSource: DebugSimEmulationSource

    @Before
    fun setUp() {
        contentResolver = mockk()
        emulationModeFlow = MutableStateFlow(DebugSimEmulationMode.DEFAULT)
        emulationSource = mockk()
        every { emulationSource.mode } returns emulationModeFlow
    }

    @After
    fun tearDown() {
        unmockkStatic(MmsConfig::class)
    }

    @Test
    fun observeActiveSubscriptions_mapsActiveSelfParticipantsAndSortsBySlot() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "self-slot-2",
                        subId = 2,
                        slotId = 1,
                        subscriptionName = "Carrier B",
                        displayDestination = "+1 555 0200",
                        subscriptionColor = 0x112233,
                    ),
                    participantRow(
                        participantId = "self-slot-1",
                        subId = 1,
                        slotId = 0,
                        subscriptionName = "Carrier A",
                        displayDestination = "+1 555 0100",
                        subscriptionColor = 0x445566,
                    ),
                ),
            )
            stubObserverRegistration()

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                assertEquals(
                    listOf(
                        Subscription(
                            selfParticipantId = "self-slot-1",
                            subId = 1,
                            label = ConversationSubscriptionLabel.Named(name = "Carrier A"),
                            displayDestination = "+1 555 0100",
                            displaySlotId = 1,
                            color = 0xFF445566.toInt(),
                        ),
                        Subscription(
                            selfParticipantId = "self-slot-2",
                            subId = 2,
                            label = ConversationSubscriptionLabel.Named(name = "Carrier B"),
                            displayDestination = "+1 555 0200",
                            displaySlotId = 2,
                            color = 0xFF112233.toInt(),
                        ),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_skipsDefaultSelfAndInactiveParticipants() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "default-self",
                        subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                        slotId = 0,
                        subscriptionName = "Default",
                        displayDestination = "+1 555 0000",
                    ),
                    participantRow(
                        participantId = "inactive",
                        subId = 5,
                        slotId = ParticipantData.INVALID_SLOT_ID,
                        subscriptionName = "Inactive",
                        displayDestination = "+1 555 0001",
                    ),
                    participantRow(
                        participantId = "active",
                        subId = 1,
                        slotId = 0,
                        subscriptionName = "Active",
                        displayDestination = "+1 555 0002",
                        subscriptionColor = 0xAABBCC,
                    ),
                ),
            )
            stubObserverRegistration()

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                assertEquals(
                    listOf(
                        Subscription(
                            selfParticipantId = "active",
                            subId = 1,
                            label = ConversationSubscriptionLabel.Named(name = "Active"),
                            displayDestination = "+1 555 0002",
                            displaySlotId = 1,
                            color = 0xFFAABBCC.toInt(),
                        ),
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_emitsSlotLabelWhenSubscriptionNameIsBlank() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "self",
                        subId = 1,
                        slotId = 2,
                        subscriptionName = "   ",
                        displayDestination = "   ",
                        subscriptionColor = 0xAABBCC,
                    ),
                ),
            )
            stubObserverRegistration()

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(1, items.size)
                assertEquals(
                    ConversationSubscriptionLabel.Slot(slotId = 3),
                    items.single().label,
                )
                assertEquals(null, items.single().displayDestination)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_defaultModeReturnsRealSubscriptionsUnchanged() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "real",
                        subId = 1,
                        slotId = 0,
                        subscriptionName = "Real",
                        displayDestination = "+1 555 0100",
                        subscriptionColor = 0x112233,
                    ),
                ),
            )
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.DEFAULT

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(1, items.size)
                assertEquals("real", items.single().selfParticipantId)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_singleModeKeepsRealSubscriptionWhenPresent() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "real",
                        subId = 1,
                        slotId = 0,
                        subscriptionName = "Real",
                        displayDestination = "+1 555 0100",
                        subscriptionColor = 0x112233,
                    ),
                ),
            )
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.SINGLE

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(1, items.size)
                assertEquals("real", items.single().selfParticipantId)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_singleModeInjectsFakeSimWhenNoRealSubscriptionsExist() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubQuery(cursor = createParticipantsCursor())
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.SINGLE

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(1, items.size)
                val fake = items.single()
                assertEquals(1, fake.displaySlotId)
                assertTrue(fake.selfParticipantId.startsWith("debug_sim_emulated_"))
                assertEquals(
                    ConversationSubscriptionLabel.DebugFake(slotId = 1),
                    fake.label,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_dualModeInjectsTwoFakeSimsWhenNoRealSubscriptionsExist() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            stubQuery(cursor = createParticipantsCursor())
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.DUAL

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(2, items.size)
                assertEquals(1, items[0].displaySlotId)
                assertEquals(2, items[1].displaySlotId)
                assertTrue(
                    items.all { it.selfParticipantId.startsWith("debug_sim_emulated_") },
                )
                assertTrue(
                    items.all { it.label is ConversationSubscriptionLabel.DebugFake },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_dualModePairsSingleRealSimWithFakeInOtherSlot() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "real",
                        subId = 1,
                        slotId = 1,
                        subscriptionName = "Real",
                        displayDestination = "+1 555 0100",
                        subscriptionColor = 0x112233,
                    ),
                ),
            )
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.DUAL

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(2, items.size)
                val bySlot = items.associateBy { it.displaySlotId }
                assertEquals("real", bySlot[2]?.selfParticipantId)
                assertTrue(
                    bySlot[1]?.selfParticipantId.orEmpty().startsWith("debug_sim_emulated_"),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_dualModePairsRealSimInSlot1WithFakeInSlot2() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "real",
                        subId = 1,
                        slotId = 0,
                        subscriptionName = "Real",
                        displayDestination = "+1 555 0100",
                        subscriptionColor = 0x112233,
                    ),
                ),
            )
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.DUAL

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(2, items.size)
                val bySlot = items.associateBy { it.displaySlotId }
                assertEquals("real", bySlot[1]?.selfParticipantId)
                assertTrue(
                    bySlot[2]?.selfParticipantId.orEmpty().startsWith("debug_sim_emulated_"),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_dualModePassesThroughWhenTwoRealSubscriptionsExist() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(
                cursor = createParticipantsCursor(
                    participantRow(
                        participantId = "real-a",
                        subId = 1,
                        slotId = 0,
                        subscriptionName = "A",
                        displayDestination = "+1 555 0100",
                        subscriptionColor = 0x112233,
                    ),
                    participantRow(
                        participantId = "real-b",
                        subId = 2,
                        slotId = 1,
                        subscriptionName = "B",
                        displayDestination = "+1 555 0200",
                        subscriptionColor = 0x445566,
                    ),
                ),
            )
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.DUAL

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                val items = awaitItem()
                assertEquals(
                    listOf("real-a", "real-b"),
                    items.map { it.selfParticipantId },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_reemitsWhenEmulationModeChanges() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            stubQuery(cursor = createParticipantsCursor())
            stubObserverRegistration()
            emulationModeFlow.value = DebugSimEmulationMode.DEFAULT

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                assertEquals(0, awaitItem().size)

                emulationModeFlow.value = DebugSimEmulationMode.SINGLE
                assertEquals(1, awaitItem().size)

                emulationModeFlow.value = DebugSimEmulationMode.DUAL
                assertEquals(2, awaitItem().size)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun observeActiveSubscriptions_registersAndUnregistersObserver() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            val registeredObserver = slot<ContentObserver>()
            val expectedUri = MessagingContentProvider.PARTICIPANTS_URI

            every {
                contentResolver.registerContentObserver(
                    expectedUri,
                    true,
                    capture(registeredObserver),
                )
            } just runs
            every { contentResolver.unregisterContentObserver(any()) } just runs
            stubQuery(cursor = createParticipantsCursor())

            val repository = createRepository()

            repository.observeActiveSubscriptions().test {
                awaitItem()
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
    fun resolveMaxMessageSize_returnsGlobalFallbackWhenSelfParticipantIdIsBlank() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            mockkStatic(MmsConfig::class)
            every { MmsConfig.getMaxMaxMessageSize() } returns 456_000

            val repository = createRepository()

            repository.resolveMaxMessageSize(selfParticipantId = "").test {
                assertEquals(456_000, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) {
                contentResolver.query(
                    any<Uri>(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }
        }
    }

    @Test
    fun resolveMaxMessageSize_usesParticipantSubscriptionMmsConfig() {
        runTest(
            context = mainDispatcherRule.testDispatcher
        ) {
            mockkStatic(MmsConfig::class)
            val mmsConfig = mockk<MmsConfig>()
            every { MmsConfig.get(7) } returns mmsConfig
            every { mmsConfig.maxMessageSize } returns 987_000
            every { MmsConfig.getMaxMaxMessageSize() } returns 123_000
            every {
                contentResolver.query(
                    MessagingContentProvider.PARTICIPANTS_URI,
                    ParticipantData.ParticipantsQuery.PROJECTION,
                    "${ParticipantColumns._ID} = ?",
                    arrayOf("self-7"),
                    null,
                )
            } returns createParticipantsCursor(
                participantRow(
                    participantId = "self-7",
                    subId = 7,
                    slotId = 0,
                    subscriptionName = "Carrier",
                    displayDestination = "+1 555 7000",
                ),
            )

            val repository = createRepository()

            repository.resolveMaxMessageSize(selfParticipantId = "self-7").test {
                assertEquals(987_000, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    private fun createRepository(): SubscriptionsRepositoryImpl {
        return SubscriptionsRepositoryImpl(
            context = mockk<Context>(),
            contentResolver = contentResolver,
            subscriptionManager = mockk<SubscriptionManager>(),
            debugSimEmulationSource = emulationSource,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            messagingDbDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun stubQuery(cursor: MatrixCursor) {
        every {
            contentResolver.query(
                MessagingContentProvider.PARTICIPANTS_URI,
                ParticipantData.ParticipantsQuery.PROJECTION,
                "${ParticipantColumns.SUB_ID} <> ?",
                arrayOf(ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
                null,
            )
        } returns cursor
    }

    private fun stubObserverRegistration() {
        every {
            contentResolver.registerContentObserver(
                any<Uri>(),
                any(),
                any(),
            )
        } just runs
        every { contentResolver.unregisterContentObserver(any()) } just runs
    }
}
