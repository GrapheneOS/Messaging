package com.android.messaging.util

import android.content.Context
import android.telephony.SubscriptionManager
import com.android.messaging.FactoryTestAccess
import com.android.messaging.testutil.installTestFactory
import java.time.Duration
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSubscriptionManager
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhoneUtilsSimLocaleTest {

    private lateinit var context: Context
    private lateinit var defaultLocale: Locale

    @Before
    fun setUp() {
        ShadowSubscriptionManager.reset()
        defaultLocale = Locale.getDefault()
        Locale.setDefault(ESTONIAN_LOCALE)
        context = RuntimeEnvironment.getApplication().applicationContext
        installTestFactory(context = context)
        givenSubscriptions(SUB_ID to "us")
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        ShadowSubscriptionManager.reset()
        FactoryTestAccess.reset()
    }

    @Test
    fun whenTheSimCountryCannotReadTheNumber_usesTheCountriesTheComposerTries() {
        assertEquals(
            "a number the SIM's country can't read must still normalize the way the composer" +
                " does, or its sender gets a participant row of its own",
            "+37254810027",
            PhoneUtils(SUB_ID).getCanonicalBySimLocale("54810027"),
        )
    }

    @Test
    fun whenTheSimCountryCanReadTheNumber_keepsTheSimCountry() {
        assertEquals(
            "+12015550123",
            PhoneUtils(SUB_ID).getCanonicalBySimLocale("2015550123"),
        )
    }

    @Test
    fun keepsAnAlphanumericSenderAsIs() {
        assertEquals("AMAZON", PhoneUtils(SUB_ID).getCanonicalBySimLocale("AMAZON"))
    }

    @Test
    fun keepsAShortCodeAsIs() {
        assertEquals("13011", PhoneUtils(SUB_ID).getCanonicalBySimLocale("13011"))
    }

    @Test
    fun keepsAShortCodeAsIsWhenAnotherCountryReadsItAsANumber() {
        givenSubscriptions(SUB_ID to "us", FAROESE_SUB_ID to "fo")

        assertEquals("211234", PhoneUtils(SUB_ID).getCanonicalBySimLocale("211234"))
    }

    @Test
    fun whenTheCountriesTheComposerTriesChange_followsThemShortlyAfter() {
        Locale.setDefault(Locale.ENGLISH)
        val phoneUtils = PhoneUtils(SUB_ID)
        assertEquals("54810029", phoneUtils.getCanonicalBySimLocale("54810029"))

        Locale.setDefault(ESTONIAN_LOCALE)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))

        assertEquals("+37254810029", phoneUtils.getCanonicalForEnteredPhoneNumber("54810029"))
        assertEquals("+37254810029", phoneUtils.getCanonicalBySimLocale("54810029"))
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

    private companion object {
        private const val SUB_ID = 2
        private const val FAROESE_SUB_ID = 3
        private val ESTONIAN_LOCALE = Locale.Builder().setLanguage("en").setRegion("EE").build()
    }
}
