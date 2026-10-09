package com.android.messaging.data.sms

import android.os.Bundle
import androidx.appcompat.mms.CarrierConfigValuesLoader
import app.cash.turbine.test
import com.android.messaging.Factory
import com.android.messaging.sms.BugleCarrierConfigValuesLoader
import com.android.messaging.sms.MmsConfig
import com.android.messaging.testutil.VERIZON_EMAIL_GATEWAY_NUMBER
import com.android.messaging.testutil.stubActiveSubscriptions
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MmsConfigLoadSourceImplTest {

    @Before
    fun setUp() {
        mockkStatic(Factory::class, PhoneUtils::class)
        val carrierConfig = Bundle().apply {
            putBoolean(CarrierConfigValuesLoader.CONFIG_ALIAS_ENABLED, true)
            putString(
                CarrierConfigValuesLoader.CONFIG_EMAIL_GATEWAY_NUMBER,
                VERIZON_EMAIL_GATEWAY_NUMBER,
            )
        }
        val carrierConfigLoader = mockk<BugleCarrierConfigValuesLoader>(relaxed = true) {
            every { get(SUB_ID) } returns carrierConfig
        }
        every { Factory.get() } returns mockk {
            every { carrierConfigValuesLoader } returns carrierConfigLoader
        }
        clearLoadedMmsConfigs()
        stubActiveSubscriptions(subIds = listOf(SUB_ID))
        val phoneUtils = PhoneUtils.getDefault()
        every { phoneUtils.getEffectiveSubId(SUB_ID) } returns SUB_ID
    }

    @After
    fun tearDown() {
        clearLoadedMmsConfigs()
        unmockkStatic(Factory::class, PhoneUtils::class)
    }

    @Test
    fun observeLoads_emitsAgainOnceTheLoadedCarrierConfigIsReadable() {
        runTest {
            MmsConfigLoadSourceImpl()
                .observeLoads()
                .map { MmsConfig.get(SUB_ID).isAliasEnabled }
                .test {
                    assertFalse(awaitItem())

                    MmsConfig.load()

                    assertTrue(awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
        }
    }

    private fun clearLoadedMmsConfigs() {
        stubActiveSubscriptions(subIds = emptyList())
        MmsConfig.load()
    }

    private companion object {
        const val SUB_ID = 1
    }
}
