package com.android.messaging.data.debugmmsconfig.repository

import android.telephony.SubscriptionInfo
import com.android.messaging.data.debugmmsconfig.model.DebugSim
import com.android.messaging.data.debugmmsconfig.model.MmsConfigEntry
import com.android.messaging.data.debugmmsconfig.model.MmsConfigKeyType
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.sms.MmsConfig
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class MmsConfigRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private val repository = MmsConfigRepositoryImpl(messagingDbDispatcher = testDispatcher)

    @Before
    fun setUp() {
        mockkStatic(MmsConfig::class)
        mockkStatic(PhoneUtils::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getActiveSims_mapsSubscriptionsWithMccMnc() = runTest(testDispatcher) {
        val subInfo = mockk<SubscriptionInfo> {
            every { subscriptionId } returns 5
        }
        val defaultPhone = mockk<PhoneUtils> {
            every { activeSubscriptionInfoList } returns listOf(subInfo)
        }
        val simPhone = mockk<PhoneUtils> {
            every { mccMnc } returns intArrayOf(310, 260)
        }
        every { PhoneUtils.getDefault() } returns defaultPhone
        every { PhoneUtils.get(5) } returns simPhone

        val result = repository.getActiveSims()
        assertEquals(
            persistentListOf(DebugSim(subId = SubId(5), mcc = 310, mnc = 260)),
            result,
        )
    }

    @Test
    fun getActiveSims_nullSubscriptions_returnsEmpty() = runTest(testDispatcher) {
        val defaultPhone = mockk<PhoneUtils> {
            every { activeSubscriptionInfoList } returns null
        }
        every { PhoneUtils.getDefault() } returns defaultPhone

        assertEquals(persistentListOf<DebugSim>(), repository.getActiveSims())
    }

    @Test
    fun getEntries_filtersUnknownKeyTypesAndSortsByKey() = runTest(testDispatcher) {
        val mmsConfig = mockk<MmsConfig>()
        every { MmsConfig.get(1) } returns mmsConfig
        every { mmsConfig.keySet() } returns linkedSetOf("maxMessageSize", "zebra", "enabledMMS")
        every { MmsConfig.getKeyType("enabledMMS") } returns MmsConfig.KEY_TYPE_BOOL
        every { MmsConfig.getKeyType("maxMessageSize") } returns MmsConfig.KEY_TYPE_INT
        every { MmsConfig.getKeyType("zebra") } returns null
        every { mmsConfig.getValue("enabledMMS") } returns true
        every { mmsConfig.getValue("maxMessageSize") } returns 1024

        val result = repository.getEntries(SubId(1))

        assertEquals(
            persistentListOf(
                MmsConfigEntry(
                    key = "enabledMMS",
                    keyType = MmsConfigKeyType.BOOL,
                    value = "true",
                ),
                MmsConfigEntry(
                    key = "maxMessageSize",
                    keyType = MmsConfigKeyType.INT,
                    value = "1024",
                ),
            ),
            result,
        )
    }

    @Test
    fun getEntries_nullValue_mapsToNullString() = runTest(testDispatcher) {
        val mmsConfig = mockk<MmsConfig>()
        every { MmsConfig.get(1) } returns mmsConfig
        every { mmsConfig.keySet() } returns linkedSetOf("userAgent")
        every { MmsConfig.getKeyType("userAgent") } returns MmsConfig.KEY_TYPE_STRING
        every { mmsConfig.getValue("userAgent") } returns null

        val result = repository.getEntries(SubId(1))
        assertEquals(
            persistentListOf(
                MmsConfigEntry(
                    key = "userAgent",
                    keyType = MmsConfigKeyType.STRING,
                    value = "null",
                ),
            ),
            result,
        )
    }

    @Test
    fun updateEntry_updatesMmsConfigWithRawType() = runTest(testDispatcher) {
        val mmsConfig = mockk<MmsConfig>(relaxed = true)
        every { MmsConfig.get(3) } returns mmsConfig

        repository.updateEntry(
            subId = SubId(3),
            key = "enabledMMS",
            keyType = MmsConfigKeyType.BOOL,
            value = "true",
        )

        verify {
            mmsConfig.update(MmsConfig.KEY_TYPE_BOOL, "enabledMMS", "true")
        }
    }
}
