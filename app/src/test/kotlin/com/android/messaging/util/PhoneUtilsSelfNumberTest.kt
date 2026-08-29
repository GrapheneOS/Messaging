package com.android.messaging.util

import android.content.Context
import android.telephony.SubscriptionManager
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
class PhoneUtilsSelfNumberTest {

    private lateinit var context: Context
    private lateinit var prefs: FakeBuglePrefs

    @Before
    fun setUp() {
        ShadowSubscriptionManager.reset()
        context = RuntimeEnvironment.getApplication().applicationContext
        prefs = FakeBuglePrefs()
        installTestFactory(context = context, prefs = prefs)
    }

    @After
    fun tearDown() {
        ShadowSubscriptionManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun getSelfRawNumberResolvesTheNumberTheCarrierKnowsWhenTheSimDoesNotCarryIt() {
        givenSubscription(numberOnSim = "")
        givenCarrierKnowsNumber(CARRIER_NUMBER)

        assertEquals(
            "the self number is read from the SIM alone, so carriers that do not write the" +
                " MSISDN to the UICC leave it unresolvable",
            CARRIER_NUMBER,
            PhoneUtils(SUB_ID).getSelfRawNumber(false),
        )
    }

    @Test
    fun getSelfRawNumberPrefersTheNumberTheUserTypedIn() {
        givenSubscription(numberOnSim = SIM_NUMBER)
        givenCarrierKnowsNumber(CARRIER_NUMBER)
        prefs.putString(context.getString(R.string.mms_phone_number_pref_key), OVERRIDE_NUMBER)

        assertEquals(
            "the number the user entered in settings must win over anything telephony reports",
            OVERRIDE_NUMBER,
            PhoneUtils(SUB_ID).getSelfRawNumber(true),
        )
    }

    @Test
    fun getSelfRawNumberFallsBackToTheSimWhenTheCarrierKnowsNoNumber() {
        givenSubscription(numberOnSim = SIM_NUMBER)

        assertEquals(
            "the SIM's own number must still be used when the carrier reports nothing",
            SIM_NUMBER,
            PhoneUtils(SUB_ID).getSelfRawNumber(false),
        )
    }

    @Test
    fun getSelfRawNumberFallsBackToTheSimWhenReadingTheCarrierNumberIsDenied() {
        givenSubscription(numberOnSim = SIM_NUMBER)
        givenCarrierKnowsNumber(CARRIER_NUMBER)
        shadowOf(subscriptionManager()).setReadPhoneNumbersPermission(false)

        assertEquals(
            "a denied carrier number read must not lose the number the SIM does carry",
            SIM_NUMBER,
            PhoneUtils(SUB_ID).getSelfRawNumber(false),
        )
    }

    @Test
    fun getSelfRawNumberStillReportsThatThereIsNoSubscriptionToReadFrom() {
        // Telephony remembers numbers for subscriptions that are no longer active, so asking it
        // first would turn "SIM is not ready" into a send that fails further down.
        givenCarrierKnowsNumber(CARRIER_NUMBER)

        assertThrows(IllegalStateException::class.java) {
            PhoneUtils(SUB_ID).getSelfRawNumber(false)
        }
    }

    private fun subscriptionManager(): SubscriptionManager {
        return context.getSystemService(SubscriptionManager::class.java)
    }

    private fun givenSubscription(numberOnSim: String) {
        shadowOf(subscriptionManager()).setActiveSubscriptionInfos(
            ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder()
                .setId(SUB_ID)
                .setNumber(numberOnSim)
                .buildSubscriptionInfo(),
        )
    }

    private fun givenCarrierKnowsNumber(number: String) {
        shadowOf(subscriptionManager()).setPhoneNumber(SUB_ID, number)
    }

    private companion object {
        private const val SUB_ID = 2
        private const val SIM_NUMBER = "+15550001111"
        private const val CARRIER_NUMBER = "+37253953334"
        private const val OVERRIDE_NUMBER = "+37255500002"
    }
}
