@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.messaging.data.conversationsettings.repository

import app.cash.turbine.test
import com.android.messaging.FactoryTestAccess
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ConversationNotificationRepositoryImplTest {

    private val prefs = FakeBuglePrefs()
    private val context = RuntimeEnvironment.getApplication()
    private val repository = ConversationNotificationRepositoryImpl(context = context)

    @Before
    fun setUp() {
        installTestFactory(context = context, prefs = prefs)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun observeIsSnoozed_turnsFalseWhenTheSnoozeExpires() = runTest {
        prefs.putLong(
            "${ConversationNotificationRepositoryImpl.SNOOZE_KEY_PREFIX}${CONVERSATION_ID.value}",
            System.currentTimeMillis() + SNOOZE_MILLIS,
        )

        repository.observeIsSnoozed(conversationId = CONVERSATION_ID).test {
            assertTrue(awaitItem())

            advanceTimeBy(delayTimeMillis = SNOOZE_MILLIS / 2)
            expectNoEvents()

            advanceTimeBy(delayTimeMillis = SNOOZE_MILLIS)
            assertFalse(awaitItem())
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation-1")
        const val SNOOZE_MILLIS = 60 * 60 * 1000L
    }
}
