package com.android.messaging.data.secondaryuser

import android.content.Context
import androidx.core.content.contentValuesOf
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationsettings.model.SnoozeOption
import com.android.messaging.data.conversationsettings.repository.ConversationNotificationRepositoryImpl
import com.android.messaging.data.secondaryuser.model.SecondaryUserNotificationTarget
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.createInMemoryActionSyncTestDatabase
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecondaryUserNotificationTargetResolverTest {

    private val context: Context = RuntimeEnvironment.getApplication().applicationContext
    private val subscriptionPhoneUtils = mockk<PhoneUtils>(relaxed = true)
    private lateinit var database: DatabaseWrapper
    private lateinit var conversationNotificationRepository: ConversationNotificationRepositoryImpl
    private lateinit var resolver: SecondaryUserNotificationTargetResolver

    @Before
    fun setUp() {
        every { subscriptionPhoneUtils.getCanonicalBySimLocale(any()) } answers { firstArg() }
        val dataModel = mockk<DataModel>(relaxed = true)
        installTestFactory(context = context, dataModel = dataModel)
        every { Factory.get().getPhoneUtils(SUBSCRIPTION_ID) } returns subscriptionPhoneUtils
        database = createInMemoryActionSyncTestDatabase(context = context)
        every { dataModel.database } returns database
        conversationNotificationRepository = ConversationNotificationRepositoryImpl(
            context = context,
        )
        resolver = SecondaryUserNotificationTargetResolverImpl(
            conversationNotificationRepository = conversationNotificationRepository,
        )
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun resolve_senderWithConversation_targetsThatConversation() {
        val conversationId = insertConversation(destination = SENDER, name = SENDER_NAME)

        assertEquals(
            SecondaryUserNotificationTarget.Conversation(
                conversationId = conversationId,
                conversationTitle = SENDER_NAME,
            ),
            resolve(address = SENDER),
        )
    }

    @Test
    fun resolve_unnamedConversation_titlesChannelWithSender() {
        val conversationId = insertConversation(destination = SENDER, name = null)

        assertEquals(
            SecondaryUserNotificationTarget.Conversation(
                conversationId = conversationId,
                conversationTitle = SENDER,
            ),
            resolve(address = SENDER),
        )
    }

    @Test
    fun resolve_normalizesSenderWithIncomingSubscription() {
        every { subscriptionPhoneUtils.getCanonicalBySimLocale(LOCAL_SENDER) } returns SENDER
        val conversationId = insertConversation(destination = SENDER, name = SENDER_NAME)

        assertEquals(
            SecondaryUserNotificationTarget.Conversation(
                conversationId = conversationId,
                conversationTitle = SENDER_NAME,
            ),
            resolve(address = LOCAL_SENDER),
        )
    }

    @Test
    fun resolve_blockedSender_isSilenced() {
        insertConversation(destination = SENDER, name = SENDER_NAME, isBlocked = true)

        assertEquals(SecondaryUserNotificationTarget.Silenced, resolve(address = SENDER))
    }

    @Test
    fun resolve_blockedSenderWithoutConversation_isSilenced() {
        insertParticipant(destination = SENDER, isBlocked = true)

        assertEquals(SecondaryUserNotificationTarget.Silenced, resolve(address = SENDER))
    }

    @Test
    fun resolve_snoozedConversation_isSilenced() {
        val conversationId = insertConversation(destination = SENDER, name = SENDER_NAME)
        conversationNotificationRepository.snooze(
            conversationId = ConversationId(conversationId),
            option = SnoozeOption.Always,
        )

        assertEquals(SecondaryUserNotificationTarget.Silenced, resolve(address = SENDER))
    }

    @Test
    fun resolve_otherConversationSnoozed_targetsSendersConversation() {
        val conversationId = insertConversation(destination = SENDER, name = SENDER_NAME)
        val otherConversationId = insertConversation(destination = OTHER_SENDER, name = null)
        conversationNotificationRepository.snooze(
            conversationId = ConversationId(otherConversationId),
            option = SnoozeOption.Always,
        )

        assertEquals(
            SecondaryUserNotificationTarget.Conversation(
                conversationId = conversationId,
                conversationTitle = SENDER_NAME,
            ),
            resolve(address = SENDER),
        )
    }

    @Test
    fun resolve_senderWithSeveralConversations_targetsTheMostRecentOne() {
        val participantId = insertParticipant(destination = SENDER, isBlocked = false)
        insertConversationFor(
            participantId = participantId,
            destination = SENDER,
            name = SENDER_NAME,
            sortTimestamp = OLDER_SORT_TIMESTAMP,
        )
        val newestConversationId = insertConversationFor(
            participantId = participantId,
            destination = SENDER,
            name = SENDER_NAME,
            sortTimestamp = NEWER_SORT_TIMESTAMP,
        )

        assertEquals(
            SecondaryUserNotificationTarget.Conversation(
                conversationId = newestConversationId,
                conversationTitle = SENDER_NAME,
            ),
            resolve(address = SENDER),
        )
    }

    @Test
    fun resolve_senderWithoutConversation_fallsBackToNoConversation() {
        insertConversation(destination = OTHER_SENDER, name = null)

        assertEquals(SecondaryUserNotificationTarget.NoConversation, resolve(address = SENDER))
    }

    @Test
    fun resolve_missingAddress_fallsBackToNoConversation() {
        insertConversation(destination = SENDER, name = SENDER_NAME)

        assertEquals(SecondaryUserNotificationTarget.NoConversation, resolve(address = null))
        assertEquals(SecondaryUserNotificationTarget.NoConversation, resolve(address = ""))
    }

    // The legacy lookups assert they run off the main thread, as the receiver does.
    private fun resolve(address: String?): SecondaryUserNotificationTarget {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor
                .submit(
                    Callable {
                        resolver.resolve(address = address, subscriptionId = SUBSCRIPTION_ID)
                    },
                )
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }
    }

    private fun insertParticipant(
        destination: String,
        isBlocked: Boolean,
    ): Long {
        return database.insert(
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

    private fun insertConversation(
        destination: String,
        name: String?,
        isBlocked: Boolean = false,
    ): String {
        return insertConversationFor(
            participantId = insertParticipant(destination = destination, isBlocked = isBlocked),
            destination = destination,
            name = name,
            sortTimestamp = 0L,
        )
    }

    private fun insertConversationFor(
        participantId: Long,
        destination: String,
        name: String?,
        sortTimestamp: Long,
    ): String {
        val conversationId = database.insert(
            DatabaseHelper.CONVERSATIONS_TABLE,
            null,
            contentValuesOf(
                ConversationColumns.NAME to name,
                ConversationColumns.PARTICIPANT_COUNT to 1,
                ConversationColumns.OTHER_PARTICIPANT_NORMALIZED_DESTINATION to destination,
                ConversationColumns.SORT_TIMESTAMP to sortTimestamp,
            ),
        )
        database.insert(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            contentValuesOf(
                ConversationParticipantsColumns.CONVERSATION_ID to conversationId,
                ConversationParticipantsColumns.PARTICIPANT_ID to participantId,
            ),
        )
        return conversationId.toString()
    }

    private companion object {
        const val SUBSCRIPTION_ID = 7
        const val SENDER = "+15551234567"
        const val LOCAL_SENDER = "5551234567"
        const val OTHER_SENDER = "+15557654321"
        const val SENDER_NAME = "Alice"
        const val TIMEOUT_SECONDS = 5L
        const val OLDER_SORT_TIMESTAMP = 1_000L
        const val NEWER_SORT_TIMESTAMP = 2_000L
    }
}
