package com.android.messaging.datamodel

import android.content.Context
import android.database.sqlite.SQLiteFullException
import android.telephony.SubscriptionManager
import androidx.core.content.contentValuesOf
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.conversationstate.BlockedNumberRecorderImpl
import com.android.messaging.data.conversationstate.ConversationStateMirror
import com.android.messaging.data.conversationstate.ConversationStateMirrorImpl
import com.android.messaging.data.conversationstate.store.ConversationStateDatabaseStoreImpl
import com.android.messaging.data.conversationstate.store.ConversationStatePreferencesStoreImpl
import com.android.messaging.data.databasecompatibility.DatabaseCompatibility
import com.android.messaging.data.participantdestination.BareNumberReaderImpl
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizer
import com.android.messaging.data.participantdestination.ParticipantDestinationNormalizerImpl
import com.android.messaging.data.participantdestination.store.ParticipantDestinationDatabaseStoreImpl
import com.android.messaging.data.participantdestination.store.ParticipantDestinationPreferencesStoreImpl
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.domain.sync.usecase.RepairAfterMessageSyncImpl
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.BuglePrefsKeys
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSubscriptionManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepairAfterMessageSyncNormalizationTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val prefs = FakeBuglePrefs()
    private val databaseVersion = context.getString(R.string.database_version).toInt()
    private val participantDestinationPreferencesStore =
        ParticipantDestinationPreferencesStoreImpl(context = context)
    private lateinit var db: DatabaseWrapper
    private lateinit var conversationStateMirror: ConversationStateMirror
    private lateinit var participantDestinationNormalizer: ParticipantDestinationNormalizer
    private lateinit var savedLocale: Locale

    @Before
    fun setUp() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(ESTONIAN_LOCALE)
        ShadowSubscriptionManager.reset()
        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        installTestFactory(context = context, prefs = prefs)
        every { Factory.get().getPhoneUtils(any()) } answers {
            when (val subId = firstArg<Int>()) {
                ParticipantData.DEFAULT_SELF_SUB_ID -> PhoneUtils(DEFAULT_SUB_ID)
                else -> PhoneUtils(subId)
            }
        }
        mockkStatic(ParticipantRefresh::class)
        every { ParticipantRefresh.refreshParticipants(any()) } just runs
        mockkStatic(MessagingContentProvider::class)
        every { MessagingContentProvider.notifyEverythingChanged() } just runs
        db = createInMemoryActionSyncTestDatabase(context = context)
        every { DataModel.get().database } returns db
        val participantDestinationDatabaseStore = ParticipantDestinationDatabaseStoreImpl()
        val bareNumberReader = BareNumberReaderImpl(
            databaseStore = participantDestinationDatabaseStore,
        )
        participantDestinationNormalizer = ParticipantDestinationNormalizerImpl(
            preferencesStore = participantDestinationPreferencesStore,
            databaseStore = participantDestinationDatabaseStore,
            bareNumberReader = bareNumberReader,
            databaseVersion = databaseVersion,
        )
        conversationStateMirror = ConversationStateMirrorImpl(
            preferencesStore = ConversationStatePreferencesStoreImpl(context = context),
            databaseStore = ConversationStateDatabaseStoreImpl(),
            blockedNumberRecorder = BlockedNumberRecorderImpl(
                preferencesStore = participantDestinationPreferencesStore,
                bareNumberReader = bareNumberReader,
            ),
            databaseVersion = databaseVersion,
        )
        mockkObject(ConversationStateMirror.Companion)
        every { ConversationStateMirror.get(any()) } answers { conversationStateMirror }
        mockkObject(ParticipantDestinationNormalizer.Companion)
        every { ParticipantDestinationNormalizer.get(any()) } answers {
            participantDestinationNormalizer
        }
        mockkObject(DatabaseCompatibility.Companion)
        every { DatabaseCompatibility.get(any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        db.database.close()
        Locale.setDefault(savedLocale)
        ShadowSubscriptionManager.reset()
        unmockkAll()
        FactoryTestAccess.reset()
    }

    @Test
    fun aShortCodeReceivedOnANonDefaultSim_keepsItsSender() {
        val sender = ParticipantData.getFromRawPhoneBySimLocale(SHORT_CODE, US_SUB_ID)
        assertEquals(SHORT_CODE, sender.normalizedDestination)
        val shortCodeId = db.insertParticipant(destination = sender.normalizedDestination)
        val faroeseId = db.insertParticipant(destination = FAROESE_NUMBER)
        val conversationId = db.insertConversation(participantId = shortCodeId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = shortCodeId,
            selfSubId = US_SUB_ID,
        )

        repairAfterMessageSync()

        assertEquals(
            "the short code's messages moved to the Faroese number $faroeseId",
            shortCodeId,
            db.senderOf(messageId = messageId),
        )
        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
    }

    @Test
    fun aNumberReceivedOnANonDefaultSim_isMergedAsThatSimReadsIt() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER)
        val twinId = db.insertParticipant(destination = CANONICAL_NUMBER)
        val conversationId = db.insertConversation(participantId = bareId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = bareId,
            selfSubId = US_SUB_ID,
        )

        repairAfterMessageSync()

        assertEquals(twinId, db.senderOf(messageId = messageId))
        assertEquals(emptyList<Long>(), db.participantIdsOf(destination = BARE_NUMBER))
    }

    @Test
    fun aSenderOutsideItsConversation_isLeftAloneWhenItsSimIsNoLongerInThePhone() {
        val sender = ParticipantData.getFromRawPhoneBySimLocale(SHORT_CODE, US_SUB_ID)
        val recipient = ParticipantData.getFromRawPhoneBySystemLocale(SHORT_CODE)
        assertEquals(FAROESE_NUMBER, recipient.normalizedDestination)
        val shortCodeId = db.insertParticipant(destination = sender.normalizedDestination)
        val faroeseId = db.insertParticipant(destination = recipient.normalizedDestination)
        val conversationId = db.insertConversation(participantId = faroeseId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = shortCodeId,
            selfSubId = US_SUB_ID,
        )
        givenSubscriptions(FAROESE_SUB_ID to "fo")

        repairAfterMessageSync()

        assertEquals(shortCodeId, db.senderOf(messageId = messageId))
        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
    }

    @Test
    fun aNumberReceivedOnASimNoLongerInThePhone_isLeftAlone() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER)
        db.insertParticipant(destination = CANONICAL_NUMBER)
        val conversationId = db.insertConversation(participantId = bareId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = bareId,
            selfSubId = REMOVED_SUB_ID,
        )

        repairAfterMessageSync()

        assertEquals(bareId, db.senderOf(messageId = messageId))
        assertEquals(listOf(bareId), db.participantIdsOf(destination = BARE_NUMBER))
    }

    @Test
    fun aNumberReceivedOnASimThatWasOff_isMergedOnceThatSimIsBack() {
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        val bareId = db.insertParticipant(destination = BARE_NUMBER)
        val twinId = db.insertParticipant(destination = CANONICAL_NUMBER)
        val conversationId = db.insertConversation(participantId = bareId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = bareId,
            selfSubId = US_SUB_ID,
        )
        repairAfterMessageSync()
        assertEquals(bareId, db.senderOf(messageId = messageId))

        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        assertEquals(twinId, db.senderOf(messageId = messageId))
        assertEquals(emptyList<Long>(), db.participantIdsOf(destination = BARE_NUMBER))
    }

    @Test
    fun aSimTakenOut_leavesTheNumbersTheSimsReadDifferentlyAlone() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
    }

    @Test
    fun aNumberReceivedOnAnUnknownSim_staysLeftAloneWhenASimIsReplaced() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertParticipant(destination = FAROESE_NUMBER)
        val conversationId = db.insertConversation(participantId = shortCodeId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        assertEquals(shortCodeId, db.senderOf(messageId = messageId))
        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
    }

    @Test
    fun aBlockedNumberWithoutMessages_staysLeftAloneWhenASimIsReplaced() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE, isBlocked = true)
        val faroeseId = db.insertParticipant(destination = FAROESE_NUMBER)
        val conversationId = db.insertConversation(participantId = faroeseId)
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
        assertFalse(
            "the Faroese number is blocked",
            offMainThread { BugleDatabaseOperations.isBlockedDestination(db, FAROESE_NUMBER) },
        )
        assertFalse(
            "the Faroese number's conversation is archived",
            db.isArchived(conversationId = conversationId),
        )
    }

    @Test
    fun aBlockDeletedBeforeASync_staysLeftAloneWhenASimIsReplaced() {
        repairAfterMessageSync()
        blockAndDeleteShortCode()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        val faroeseId = db.insertParticipant(destination = FAROESE_NUMBER)
        val conversationId = db.insertConversation(participantId = faroeseId)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aBlockDeletedBeforeASync_staysLeftAloneWhenRebuiltWithAnotherSim() {
        repairAfterMessageSync()
        blockAndDeleteShortCode()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        val conversationId = rebuildAndResync(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aNumberReceivedOnAnUnknownSimBeforeASync_staysLeftAloneWhenASimIsReplaced() {
        repairAfterMessageSync()
        val messageId = resyncFromAnUnknownSim(destination = SHORT_CODE)

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        db.insertParticipant(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        val shortCodeIds = db.participantIdsOf(destination = SHORT_CODE)
        assertEquals(1, shortCodeIds.size)
        assertEquals(shortCodeIds.single(), db.senderOf(messageId = messageId))
    }

    @Test
    fun aBlockDeletedWhileItsSimWasIn_staysLeftAloneWhenASimIsAdded() {
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()
        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        blockAndDeleteShortCode()
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        val faroeseId = db.insertParticipant(destination = FAROESE_NUMBER)
        val conversationId = db.insertConversation(participantId = faroeseId)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aBlockDeletedWhileItsSimWasIn_staysLeftAloneWhenRebuilt() {
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()
        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        blockAndDeleteShortCode()

        givenSubscriptions(FAROESE_SUB_ID to "fo")
        val conversationId = rebuildAndResync(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aBlockDeletedWhileItsSimWasIn_staysLeftAloneWhenRebuiltByAnOlderVersionAfterASync() {
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()
        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        blockAndDeleteShortCode()
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        val conversationId = rebuildByAnOlderVersionAndUpgrade(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aBlockDeletedWhileARestoreIsPending_staysLeftAloneOnceTheSyncCompletes() {
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        db.insertParticipant(destination = CANONICAL_NUMBER, isBlocked = true)
        repairAfterMessageSync()
        rebuildTables()
        assertTrue(
            "no restore is pending",
            conversationStateMirror.isRestorePending(),
        )

        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        blockAndDeleteShortCode()
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        val conversationId = resyncSender(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aBlockReceivedOnBothSims_staysLeftAloneWhenRebuiltAfterItsUsMessageWasDeleted() {
        repairAfterMessageSync()
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        val shortCodeConversationId = db.insertConversation(participantId = shortCodeId)
        val usMessageId = db.insertMessage(
            conversationId = shortCodeConversationId,
            senderId = shortCodeId,
            selfSubId = US_SUB_ID,
        )
        db.insertMessage(
            conversationId = shortCodeConversationId,
            senderId = shortCodeId,
            selfSubId = FAROESE_SUB_ID,
        )
        repairAfterMessageSync()
        offMainThread {
            BugleDatabaseOperations.updateDestination(db, SHORT_CODE, true)
            conversationStateMirror.update()
            BugleDatabaseOperations.deleteMessage(db, usMessageId.toString())
        }

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()
        val conversationId = rebuildAndResync(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aNumberReceivedOnAnUnknownSim_staysLeftAloneAfterAnUpgradeFromThisVersion() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertParticipant(destination = FAROESE_NUMBER)
        val conversationId = db.insertConversation(participantId = shortCodeId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo")
        participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = databaseVersion)
        repairAfterMessageSync()

        assertEquals(shortCodeId, db.senderOf(messageId = messageId))
        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
    }

    @Test
    fun aBlockedNumberWithoutMessages_staysLeftAloneWhenRebuiltWithAnotherSim() {
        db.insertParticipant(destination = SHORT_CODE, isBlocked = true)
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        val conversationId = rebuildAndResync(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aBlockedNumberReceivedOnAKnownSim_staysLeftAloneWhenRebuiltWithAnotherSim() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE, isBlocked = true)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = shortCodeId),
            senderId = shortCodeId,
            selfSubId = US_SUB_ID,
        )
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        val conversationId = rebuildAndResync(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aBlockedNumberWithoutMessages_staysLeftAloneWhenAnOlderVersionRebuiltWithAnotherSim() {
        db.insertParticipant(destination = SHORT_CODE, isBlocked = true)
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        val conversationId = rebuildByAnOlderVersionAndUpgrade(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aNumberReceivedOnAnUnknownSim_staysLeftAloneWhenAnOlderVersionRebuiltWithAnotherSim() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = shortCodeId),
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        rebuildByAnOlderVersionAndUpgrade(destination = FAROESE_NUMBER)
        val messageId = resyncFromAnUnknownSim(destination = SHORT_CODE)
        repairAfterMessageSync()

        assertEquals(
            db.participantIdsOf(destination = SHORT_CODE),
            listOf(db.senderOf(messageId = messageId)),
        )
    }

    @Test
    fun aBlockedNumberAddedAfterARepair_staysLeftAloneWhenRebuiltWithAnotherSim() {
        repairAfterMessageSync()
        db.insertParticipant(destination = SHORT_CODE, isBlocked = true)
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        val conversationId = rebuildAndResync(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertShortCodeKeepsItsBlock(conversationId = conversationId)
    }

    @Test
    fun aSenderAddedAfterARepair_staysLeftAloneWhenAnOlderVersionRebuiltWithAnotherSim() {
        repairAfterMessageSync()
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = shortCodeId),
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo", NEW_FAROESE_SUB_ID to "fo")
        rebuildByAnOlderVersionAndUpgrade(destination = FAROESE_NUMBER)
        val messageId = resyncFromAnUnknownSim(destination = SHORT_CODE)
        repairAfterMessageSync()

        assertEquals(
            db.participantIdsOf(destination = SHORT_CODE),
            listOf(db.senderOf(messageId = messageId)),
        )
    }

    @Test
    fun aNumberReceivedOnAnUnknownSim_staysLeftAloneWhenRebuiltAfterItsSimWasReplaced() {
        givenSubscriptions(US_SUB_ID to "us")
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = shortCodeId),
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()

        givenSubscriptions(FAROESE_SUB_ID to "fo")
        rebuildAndResync(destination = FAROESE_NUMBER)
        val messageId = resyncFromAnUnknownSim(destination = SHORT_CODE)
        repairAfterMessageSync()

        assertEquals(
            db.participantIdsOf(destination = SHORT_CODE),
            listOf(db.senderOf(messageId = messageId)),
        )
    }

    @Test
    fun aNumberReceivedOnAnUnknownSim_staysLeftAloneWhenAnotherIsBlockedBeforeItsResync() {
        givenSubscriptions(US_SUB_ID to "us")
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = shortCodeId),
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        rebuildAndResync(destination = BARE_NUMBER)
        offMainThread {
            BugleDatabaseOperations.updateDestination(db, BARE_NUMBER, true)
            conversationStateMirror.update()
        }
        val messageId = resyncFromAnUnknownSim(destination = SHORT_CODE)
        repairAfterMessageSync()

        assertEquals(
            db.participantIdsOf(destination = SHORT_CODE),
            listOf(db.senderOf(messageId = messageId)),
        )
    }

    @Test
    fun aNumberMergedFromAnUnknownSim_staysLeftAloneWhenRebuiltAfterItsSimWasReplaced() {
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertParticipant(destination = FAROESE_NUMBER)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = shortCodeId),
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()

        givenSubscriptions(GREENLANDIC_SUB_ID to "gl")
        rebuildAndResync(destination = FAROESE_NUMBER)
        val messageId = resyncFromAnUnknownSim(destination = SHORT_CODE)
        repairAfterMessageSync()

        assertEquals(
            db.participantIdsOf(destination = SHORT_CODE),
            listOf(db.senderOf(messageId = messageId)),
        )
    }

    @Test
    fun aNumberTheSyncDoesNotBringBack_isForgottenOnceTheTablesAreRebuilt() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = shortCodeId),
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
        repairAfterMessageSync()
        assertTrue(
            "the reading of the short code isn't kept",
            SHORT_CODE in recordedReadingDestinations(),
        )

        rebuildAndResync(destination = FAROESE_NUMBER)
        repairAfterMessageSync()

        assertFalse(SHORT_CODE in recordedReadingDestinations())
    }

    @Test
    fun aNumberReceivedOnAnUnknownSim_isLeftAloneWhenTheSimsReadItDifferently() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        db.insertParticipant(destination = FAROESE_NUMBER)
        val conversationId = db.insertConversation(participantId = shortCodeId)
        val messageId = db.insertMessage(
            conversationId = conversationId,
            senderId = shortCodeId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )

        repairAfterMessageSync()

        assertEquals(shortCodeId, db.senderOf(messageId = messageId))
        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
    }

    @Test
    fun aNumberWithoutMessages_isLeftAloneWhenTheSimsReadItDifferently() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)

        repairAfterMessageSync()

        assertEquals(listOf(shortCodeId), db.participantIdsOf(destination = SHORT_CODE))
    }

    @Test
    fun aNumberWithoutMessages_isNormalizedWhenEverySimReadsItTheSame() {
        val bareId = db.insertParticipant(destination = BARE_NUMBER)

        repairAfterMessageSync()

        assertEquals(listOf(bareId), db.participantIdsOf(destination = CANONICAL_NUMBER))
    }

    @Test
    fun beforeAnySimIsLoaded_theRenormalizationWaitsForOne() {
        givenSubscriptions()
        val bareId = db.insertParticipant(destination = BARE_NUMBER)

        repairAfterMessageSync()

        assertEquals(listOf(bareId), db.participantIdsOf(destination = BARE_NUMBER))

        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        assertEquals(listOf(bareId), db.participantIdsOf(destination = CANONICAL_NUMBER))
    }

    @Test
    fun aMirroredBareBlock_followsTheSenderToItsNormalizedNumber() {
        givenSubscriptions(US_SUB_ID to "us")
        Locale.setDefault(Locale.ENGLISH)
        val blockedId = db.insertParticipant(destination = BLOCKED_BARE_NUMBER, isBlocked = true)
        db.insertConversation(participantId = blockedId, isArchived = true)
        repairAfterMessageSync()
        assertEquals(listOf(blockedId), db.participantIdsOf(destination = BLOCKED_BARE_NUMBER))

        val conversationId = rebuildByAnOlderVersionAndUpgrade(destination = BLOCKED_BARE_NUMBER)
        Locale.setDefault(ESTONIAN_LOCALE)
        repairAfterMessageSync()

        assertBlockedAndArchived(conversationId = conversationId)
        repairAfterMessageSync()
        assertBlockedAndArchived(conversationId = conversationId)
    }

    @Test
    fun aMirroredBareBlock_archivesTheConversationOfTheNormalizedNumber() {
        givenSubscriptions(US_SUB_ID to "us")
        Locale.setDefault(Locale.ENGLISH)
        val blockedId = db.insertParticipant(destination = BLOCKED_BARE_NUMBER, isBlocked = true)
        db.insertConversation(participantId = blockedId, isArchived = true)
        repairAfterMessageSync()

        val conversationId = rebuildByAnOlderVersionAndUpgrade(
            destination = BLOCKED_CANONICAL_NUMBER,
        )
        Locale.setDefault(ESTONIAN_LOCALE)
        repairAfterMessageSync()

        assertBlockedAndArchived(conversationId = conversationId)
    }

    @Test
    fun aMirroredBareBlock_followsTheSenderAgainOnceTheTablesAreRebuilt() {
        givenSubscriptions(US_SUB_ID to "us")
        Locale.setDefault(Locale.ENGLISH)
        val blockedId = db.insertParticipant(destination = BLOCKED_BARE_NUMBER, isBlocked = true)
        db.insertConversation(participantId = blockedId, isArchived = true)
        repairAfterMessageSync()

        Locale.setDefault(ESTONIAN_LOCALE)
        val conversationId = rebuildAndResync(destination = BLOCKED_CANONICAL_NUMBER)
        repairAfterMessageSync()

        assertBlockedAndArchived(conversationId = conversationId)
    }

    @Test
    fun aRestoredBlock_followsTheSenderOnceItsSimIsBack() {
        givenSubscriptions(FAROESE_SUB_ID to "fo")
        val blockedId = db.insertParticipant(destination = BLOCKED_BARE_NUMBER, isBlocked = true)
        db.insertMessage(
            conversationId = db.insertConversation(participantId = blockedId, isArchived = true),
            senderId = blockedId,
            selfSubId = US_SUB_ID,
        )
        repairAfterMessageSync()
        val conversationId = rebuildAndResync(destination = BLOCKED_CANONICAL_NUMBER)
        repairAfterMessageSync()
        assertFalse(
            "the block was carried without the US SIM",
            offMainThread {
                BugleDatabaseOperations.isBlockedDestination(db, BLOCKED_CANONICAL_NUMBER)
            },
        )

        givenSubscriptions(US_SUB_ID to "us", FAROESE_SUB_ID to "fo")
        repairAfterMessageSync()

        assertBlockedAndArchived(conversationId = conversationId)
    }

    @Test
    fun aRestoreThatFailed_isRetriedBeforeTheSenderIsRenormalized() {
        givenSubscriptions(US_SUB_ID to "us")
        Locale.setDefault(Locale.ENGLISH)
        val blockedId = db.insertParticipant(destination = BLOCKED_BARE_NUMBER, isBlocked = true)
        db.insertConversation(participantId = blockedId, isArchived = true)
        repairAfterMessageSync()
        val conversationId = rebuildByAnOlderVersionAndUpgrade(destination = BLOCKED_BARE_NUMBER)
        Locale.setDefault(ESTONIAN_LOCALE)
        conversationStateMirror = spyk(conversationStateMirror)
        every { conversationStateMirror.restoreIfDue() } throws
            SQLiteFullException("database or disk is full") andThenAnswer { callOriginal() }

        repairAfterMessageSync()
        repairAfterMessageSync()

        assertBlockedAndArchived(conversationId = conversationId)
    }

    @Test
    fun aMirroredNormalizedBlock_blocksTheBareNumberAnOlderVersionBroughtBack() {
        givenSubscriptions(US_SUB_ID to "us")
        val blockedId = db.insertParticipant(
            destination = BLOCKED_CANONICAL_NUMBER,
            isBlocked = true,
        )
        db.insertConversation(participantId = blockedId, isArchived = true)
        repairAfterMessageSync()

        val conversationId = rebuildByAnOlderVersionAndUpgrade(destination = BLOCKED_BARE_NUMBER)
        repairAfterMessageSync()

        assertBlockedAndArchived(conversationId = conversationId)
        repairAfterMessageSync()
        assertBlockedAndArchived(conversationId = conversationId)
    }

    private fun rebuildByAnOlderVersionAndUpgrade(destination: String): Long {
        DatabaseHelper.rebuildTables(db.database)
        val conversationId = resyncSender(destination = destination)
        conversationStateMirror.onDatabaseUpgraded(oldVersion = OLDEST_RELEASED_VERSION)
        participantDestinationNormalizer.onDatabaseUpgraded(oldVersion = OLDEST_RELEASED_VERSION)
        return conversationId
    }

    private fun rebuildAndResync(destination: String): Long {
        rebuildTables()
        return resyncSender(destination = destination)
    }

    private fun rebuildTables() {
        every { DataModel.get().onCreateTables(any()) } answers {
            DataModelImpl(context).onCreateTables(firstArg())
        }
        DatabaseHelper.rebuildTables(db.database)
    }

    private fun resyncSender(destination: String): Long {
        val senderId = db.insertParticipant(destination = destination)
        val conversationId = db.insertConversation(
            participantId = senderId,
            sortTimestamp = System.currentTimeMillis() + ONE_DAY_MILLIS,
        )
        db.insertMessage(
            conversationId = conversationId,
            senderId = senderId,
            selfSubId = US_SUB_ID,
        )
        prefs.putLong(BuglePrefsKeys.LAST_FULL_SYNC_TIME, System.currentTimeMillis())
        return conversationId
    }

    private fun blockAndDeleteShortCode() {
        val shortCodeId = db.insertParticipant(destination = SHORT_CODE)
        val conversationId = db.insertConversation(participantId = shortCodeId)
        db.insertMessage(
            conversationId = conversationId,
            senderId = shortCodeId,
            selfSubId = US_SUB_ID,
        )
        offMainThread {
            BugleDatabaseOperations.updateDestination(db, SHORT_CODE, true)
            conversationStateMirror.update()
            BugleDatabaseOperations.deleteConversation(
                db,
                conversationId.toString(),
                Long.MAX_VALUE,
            )
        }
    }

    private fun resyncFromAnUnknownSim(destination: String): Long {
        val senderId = db.insertParticipant(destination = destination)
        return db.insertMessage(
            conversationId = db.insertConversation(participantId = senderId),
            senderId = senderId,
            selfSubId = ParticipantData.DEFAULT_SELF_SUB_ID,
        )
    }

    private fun assertShortCodeKeepsItsBlock(conversationId: Long) {
        assertTrue(
            "the short code isn't blocked",
            offMainThread { BugleDatabaseOperations.isBlockedDestination(db, SHORT_CODE) },
        )
        assertFalse(
            "the Faroese number is blocked",
            offMainThread { BugleDatabaseOperations.isBlockedDestination(db, FAROESE_NUMBER) },
        )
        assertFalse(
            "the Faroese number's conversation is archived",
            db.isArchived(conversationId = conversationId),
        )
    }

    private fun assertBlockedAndArchived(conversationId: Long) {
        assertTrue(
            "an incoming message from the sender isn't blocked",
            offMainThread {
                BugleDatabaseOperations.isBlockedDestination(db, BLOCKED_CANONICAL_NUMBER)
            },
        )
        assertEquals(emptyList<Long>(), db.participantIdsOf(destination = BLOCKED_BARE_NUMBER))
        assertTrue(
            "the conversation is in the inbox",
            db.isArchived(conversationId = conversationId),
        )
    }

    private fun recordedReadingDestinations(): Set<String> {
        return participantDestinationPreferencesStore.readSimHistory().recordedReadings.keys
    }

    private fun repairAfterMessageSync() {
        offMainThread {
            RepairAfterMessageSyncImpl(
                conversationStateMirror = conversationStateMirror,
                participantDestinationNormalizer = participantDestinationNormalizer,
            ).invoke()
        }
    }

    // Updating a conversation's name asserts it runs off the main thread
    private fun <T> offMainThread(block: () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor.submit(Callable { block() }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }
    }

    private fun givenSubscriptions(vararg countriesBySubId: Pair<Int, String>) {
        shadowOf(context.getSystemService(SubscriptionManager::class.java))
            .setActiveSubscriptionInfoList(
                countriesBySubId.map { (subId, country) ->
                    ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder()
                        .setId(subId)
                        .setCountryIso(country)
                        .buildSubscriptionInfo()
                },
            )
    }

    private fun DatabaseWrapper.insertParticipant(
        destination: String,
        isBlocked: Boolean = false,
    ): Long {
        return insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ParticipantColumns.SUB_ID to ParticipantData.OTHER_THAN_SELF_SUB_ID,
                ParticipantColumns.NORMALIZED_DESTINATION to destination,
                ParticipantColumns.SEND_DESTINATION to destination,
                ParticipantColumns.DISPLAY_DESTINATION to destination,
                ParticipantColumns.BLOCKED to if (isBlocked) 1 else 0,
            ),
        )
    }

    private fun DatabaseWrapper.insertConversation(
        participantId: Long,
        isArchived: Boolean = false,
        sortTimestamp: Long = SORT_TIMESTAMP,
    ): Long {
        val conversationId = insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.SMS_THREAD_ID to THREAD_ID,
                ConversationColumns.ARCHIVE_STATUS to if (isArchived) 1 else 0,
                ConversationColumns.SORT_TIMESTAMP to sortTimestamp,
                ConversationColumns.PARTICIPANT_COUNT to 1,
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to
                    participantDestinationOf(participantId = participantId),
            ),
        )
        insert(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                ConversationParticipantsColumns.PARTICIPANT_ID to participantId,
            ),
        )
        return conversationId
    }

    private fun DatabaseWrapper.insertMessage(
        conversationId: Long,
        senderId: Long,
        selfSubId: Int,
    ): Long {
        return insert(
            DatabaseHelper.MESSAGES_TABLE,
            null,
            contentValuesOf(
                MessageColumns.CONVERSATION_ID to conversationId,
                MessageColumns.SENDER_PARTICIPANT_ID to senderId,
                MessageColumns.SELF_PARTICIPANT_ID to selfIdOf(subId = selfSubId),
            ),
        )
    }

    private fun DatabaseWrapper.selfIdOf(subId: Int): Long {
        val selfIds = queryLongs(
            sql = "SELECT _id FROM ${DatabaseHelper.PARTICIPANTS_TABLE} " +
                "WHERE ${ParticipantColumns.SUB_ID}=?",
            argument = subId.toString(),
        )
        return selfIds.firstOrNull() ?: insert(
            DatabaseHelper.PARTICIPANTS_TABLE,
            null,
            contentValuesOf(ParticipantColumns.SUB_ID to subId),
        )
    }

    private fun DatabaseWrapper.participantDestinationOf(participantId: Long): String {
        return rawQuery(
            "SELECT ${ParticipantColumns.NORMALIZED_DESTINATION} " +
                "FROM ${DatabaseHelper.PARTICIPANTS_TABLE} WHERE _id=?",
            arrayOf(participantId.toString()),
        ).use { cursor ->
            assertTrue("no participant $participantId", cursor.moveToFirst())
            cursor.getString(0)
        }
    }

    private fun DatabaseWrapper.participantIdsOf(destination: String): List<Long> {
        return queryLongs(
            sql = "SELECT _id FROM ${DatabaseHelper.PARTICIPANTS_TABLE} " +
                "WHERE ${ParticipantColumns.NORMALIZED_DESTINATION}=? " +
                "AND ${ParticipantColumns.SUB_ID}=${ParticipantData.OTHER_THAN_SELF_SUB_ID}",
            argument = destination,
        )
    }

    private fun DatabaseWrapper.senderOf(messageId: Long): Long {
        return queryLongs(
            sql = "SELECT ${MessageColumns.SENDER_PARTICIPANT_ID} " +
                "FROM ${DatabaseHelper.MESSAGES_TABLE} WHERE _id=?",
            argument = messageId.toString(),
        ).single()
    }

    private fun DatabaseWrapper.isArchived(conversationId: Long): Boolean {
        return queryLongs(
            sql = "SELECT ${ConversationColumns.ARCHIVE_STATUS} " +
                "FROM ${DatabaseHelper.CONVERSATIONS_TABLE} WHERE _id=?",
            argument = conversationId.toString(),
        ).single() == 1L
    }

    private fun DatabaseWrapper.queryLongs(sql: String, argument: String): List<Long> {
        return rawQuery(sql, arrayOf(argument)).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(0))
                }
            }
        }
    }

    private companion object {
        private const val US_SUB_ID = 2
        private const val FAROESE_SUB_ID = 3
        private const val NEW_FAROESE_SUB_ID = 4
        private const val REMOVED_SUB_ID = 5
        private const val GREENLANDIC_SUB_ID = 6

        private const val DEFAULT_SUB_ID = FAROESE_SUB_ID

        private const val SHORT_CODE = "211234"
        private const val FAROESE_NUMBER = "+298211234"
        private const val BARE_NUMBER = "54810027"
        private const val CANONICAL_NUMBER = "+37254810027"
        private const val BLOCKED_BARE_NUMBER = "54810028"
        private const val BLOCKED_CANONICAL_NUMBER = "+37254810028"

        private const val THREAD_ID = 42L
        private const val SORT_TIMESTAMP = 1_000L
        private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000
        private const val OLDEST_RELEASED_VERSION = 2
        private const val TIMEOUT_SECONDS = 10L

        private val ESTONIAN_LOCALE = Locale.Builder().setLanguage("en").setRegion("EE").build()
    }
}
