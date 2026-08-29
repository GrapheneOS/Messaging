package com.android.messaging.domain.subscriptionsettings.usecase

import android.content.Context
import android.telephony.SubscriptionManager
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSubscriptionManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SetSubscriptionPhoneNumberTest {

    private lateinit var context: Context
    private lateinit var prefs: FakeBuglePrefs
    private lateinit var prefKey: String

    @Before
    fun setUp() {
        ShadowSubscriptionManager.reset()
        context = RuntimeEnvironment.getApplication().applicationContext
        prefs = FakeBuglePrefs()
        prefKey = context.getString(R.string.mms_phone_number_pref_key)
        installTestFactory(context = context, prefs = prefs)
        givenSimOnEstonianCarrier()
        every { Factory.get().getPhoneUtils(any()) } returns PhoneUtils(SUB_ID)
    }

    @After
    fun tearDown() {
        ShadowSubscriptionManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun invokeDoesNotStoreTextThatIsNotAPhoneNumber() = runTest {
        setPhoneNumber("DROP TABLE messages")

        assertNull(
            "anything stored here becomes the sender identity of every outgoing MMS," +
                " so text that is not a phone number must never reach the preference",
            prefs.getString(prefKey, null),
        )
    }

    @Test
    fun invokeStoresAValidNumberInE164() = runTest {
        setPhoneNumber("5555 0001")

        assertEquals(
            "the stored number is written into the MMS From: header, so it has to be" +
                " normalised rather than kept as typed",
            "+37255550001",
            prefs.getString(prefKey, null),
        )
    }

    @Test
    fun invokeStoresANumberLibphonenumberOnlyConsidersPossible() {
        runTest {
            setPhoneNumber(POSSIBLE_BUT_UNASSIGNED_NUMBER)

            assertEquals(
                "libphonenumber's metadata trails real numbering plans, and this preference is" +
                    " the only way to set a number the SIM does not carry, so a number of a" +
                    " plausible length is stored rather than refused",
                "+3721234567",
                prefs.getString(prefKey, null),
            )
        }
    }

    @Test
    fun invokeClearsTheOverrideWhenTheFieldIsEmptied() = runTest {
        prefs.putString(prefKey, "+3725550001")

        setPhoneNumber("")

        assertNull(
            "emptying the field is the documented way back to the number the SIM reports",
            prefs.getString(prefKey, null),
        )
    }

    @Test
    fun invokeClearsTheOverrideWhenTheNumberMatchesTheSim() = runTest {
        prefs.putString(prefKey, "+3725550001")

        setPhoneNumber(SIM_NUMBER)

        assertNull(
            "an override that repeats the SIM's own number is not an override",
            prefs.getString(prefKey, null),
        )
    }

    private suspend fun TestScope.setPhoneNumber(phoneNumber: String) {
        SetSubscriptionPhoneNumberImpl(
            context = context,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        ).invoke(subId = SubId(SUB_ID), phoneNumber = phoneNumber)
    }

    private fun givenSimOnEstonianCarrier() {
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfos(
            ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder()
                .setId(SUB_ID)
                .setCountryIso("ee")
                .setNumber(SIM_NUMBER)
                .buildSubscriptionInfo(),
        )
    }

    private companion object {
        private const val SUB_ID = 2
        private const val SIM_NUMBER = "+37253953334"

        /** Estonian landline length, on a prefix the numbering plan does not assign. */
        private const val POSSIBLE_BUT_UNASSIGNED_NUMBER = "1234567"
    }
}
