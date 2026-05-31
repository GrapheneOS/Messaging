package com.android.messaging.data.subscriptionsettings.repository.subscriptionsettings

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.telephony.SubscriptionManager
import app.cash.turbine.test
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.data.subscriptionsettings.model.SubscriptionBooleanPref
import com.android.messaging.data.subscriptionsettings.repository.SubscriptionSettingsRepositoryImpl
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.sms.MmsConfig
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.createParticipantsCursor
import com.android.messaging.testutil.participantRow
import com.android.messaging.ui.UIIntents
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.concurrent.Executor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class SubscriptionSettingsRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var contentResolver: ContentResolver
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var packageManager: PackageManager
    private lateinit var factory: Factory
    private lateinit var defaultPhoneUtils: PhoneUtils

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        resources = mockk()
        contentResolver = mockk(relaxed = true)
        subscriptionManager = mockk()
        packageManager = mockk()
        factory = mockk(relaxed = true)
        defaultPhoneUtils = mockk()

        mockkStatic(Factory::class)
        mockkStatic(PhoneUtils::class)
        mockkStatic(MmsConfig::class)
        mockkStatic(BuglePrefs::class)

        every { Factory.get() } returns factory
        every { PhoneUtils.getDefault() } returns defaultPhoneUtils
        every { context.resources } returns resources
        every { context.getString(R.string.mms_phone_number_pref_key) } returns
            MMS_PHONE_NUMBER_PREF_KEY
        every { context.getString(R.string.group_mms_pref_key) } returns GROUP_MMS_PREF_KEY
        every { context.getString(R.string.auto_retrieve_mms_pref_key) } returns
            AUTO_RETRIEVE_MMS_PREF_KEY
        every { context.getString(R.string.auto_retrieve_mms_when_roaming_pref_key) } returns
            AUTO_RETRIEVE_MMS_WHEN_ROAMING_PREF_KEY
        every { context.getString(R.string.delivery_reports_pref_key) } returns
            DELIVERY_REPORTS_PREF_KEY
        every { resources.getBoolean(R.bool.group_mms_pref_default) } returns true
        every { resources.getBoolean(R.bool.auto_retrieve_mms_pref_default) } returns true
        every { resources.getBoolean(R.bool.auto_retrieve_mms_when_roaming_pref_default) } returns
            false
        every { resources.getBoolean(R.bool.delivery_reports_pref_default) } returns false
        every { context.packageManager } returns packageManager
        every { context.mainExecutor } returns Executor { runnable -> runnable.run() }
        every {
            packageManager.getApplicationEnabledSetting(UIIntents.CMAS_COMPONENT)
        } returns PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun isMultiSim_reflectsActiveSubscriptionCount() {
        every { defaultPhoneUtils.activeSubscriptionCount } returnsMany listOf(1, 2)
        val repository = createRepository()

        assertFalse(repository.isMultiSim())
        assertTrue(repository.isMultiSim())
    }

    @Test
    fun observeSubscriptionsChanged_registersListenerAndRemovesItOnCancel() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val listenerSlot = slot<SubscriptionManager.OnSubscriptionsChangedListener>()
            every {
                subscriptionManager.addOnSubscriptionsChangedListener(
                    any(),
                    capture(listenerSlot),
                )
            } just runs
            every {
                subscriptionManager.removeOnSubscriptionsChangedListener(any())
            } just runs
            val repository = createRepository()

            repository.observeSubscriptionsChanged().test {
                listenerSlot.captured.onSubscriptionsChanged()
                assertEquals(Unit, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 1) {
                subscriptionManager.addOnSubscriptionsChangedListener(
                    any(),
                    listenerSlot.captured,
                )
            }
            verify(exactly = 1) {
                subscriptionManager.removeOnSubscriptionsChangedListener(listenerSlot.captured)
            }
        }
    }

    @Test
    fun getSubscriptionSettings_readsDefaultSubscriptionForSingleSim() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            every { defaultPhoneUtils.activeSubscriptionCount } returns 1
            every { defaultPhoneUtils.isDefaultSmsApp } returns true
            stubPerSubscriptionData(
                subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                savedPhoneNumber = "+15550100",
                defaultPhoneNumber = "+15550200",
                formattedSavedPhoneNumber = "(555) 0100",
                formattedDefaultPhoneNumber = "(555) 0200",
                isGroupMmsSupported = true,
                isGroupMmsEnabled = false,
                autoRetrieveMms = true,
                autoRetrieveMmsWhenRoaming = false,
                isDeliveryReportsSupported = true,
                deliveryReportsEnabled = true,
                showCellBroadcast = true,
            )

            val result = createRepository().getSubscriptionSettings()

            assertTrue(result.isDefaultSmsApp)
            assertEquals(1, result.activeSubscriptionCount)
            assertTrue(result.isCellBroadcastAppEnabled)
            assertEquals(ParticipantData.DEFAULT_SELF_SUB_ID, result.defaultSelfSubscription.subId)
            assertEquals("+15550100", result.defaultSelfSubscription.savedPhoneNumber)
            assertEquals("+15550200", result.defaultSelfSubscription.defaultPhoneNumber)
            assertEquals("(555) 0100", result.defaultSelfSubscription.formattedSavedPhoneNumber)
            assertEquals("(555) 0200", result.defaultSelfSubscription.formattedDefaultPhoneNumber)
            assertTrue(result.defaultSelfSubscription.isGroupMmsSupported)
            assertFalse(result.defaultSelfSubscription.isGroupMmsEnabled)
            assertTrue(result.defaultSelfSubscription.autoRetrieveMms)
            assertFalse(result.defaultSelfSubscription.autoRetrieveMmsWhenRoaming)
            assertTrue(result.defaultSelfSubscription.isDeliveryReportsSupported)
            assertTrue(result.defaultSelfSubscription.deliveryReportsEnabled)
            assertTrue(result.defaultSelfSubscription.showCellBroadcast)
            assertTrue(result.nonDefaultActiveSelfSubscriptions.isEmpty())
            verify(exactly = 0) {
                contentResolver.query(any(), any(), any(), any(), any())
            }
        }
    }

    @Test
    fun getSubscriptionSettings_readsOnlyActiveNonDefaultSubscriptionsForMultiSim() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val projectionSlot = slot<Array<String>>()
            val selectionSlot = slot<String>()
            val selectionArgsSlot = slot<Array<String>>()
            every { defaultPhoneUtils.activeSubscriptionCount } returns 3
            every { defaultPhoneUtils.isDefaultSmsApp } returns false
            every {
                contentResolver.query(
                    MessagingContentProvider.PARTICIPANTS_URI,
                    capture(projectionSlot),
                    capture(selectionSlot),
                    capture(selectionArgsSlot),
                    null,
                )
            } returns createParticipantsCursor(
                participantRow(
                    participantId = "default-self",
                    subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                    slotId = 0,
                    subscriptionName = "Default",
                ),
                participantRow(
                    participantId = "inactive-self",
                    subId = 8,
                    slotId = ParticipantData.INVALID_SLOT_ID,
                    subscriptionName = "Inactive",
                ),
                participantRow(
                    participantId = "active-self",
                    subId = 7,
                    slotId = 1,
                    subscriptionName = "Carrier B",
                ),
            )
            stubPerSubscriptionData(
                subId = ParticipantData.DEFAULT_SELF_SUB_ID,
            )
            stubPerSubscriptionData(
                subId = 7,
                savedPhoneNumber = "",
                defaultPhoneNumber = "",
            )

            val result = createRepository().getSubscriptionSettings()

            assertFalse(result.isDefaultSmsApp)
            assertEquals(3, result.activeSubscriptionCount)
            assertEquals(
                ParticipantData.ParticipantsQuery.PROJECTION.toList(),
                projectionSlot.captured.toList(),
            )
            assertEquals(
                "${ParticipantColumns.SUB_ID} <> ?",
                selectionSlot.captured,
            )
            assertEquals(
                listOf(ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
                selectionArgsSlot.captured.toList(),
            )
            assertEquals(1, result.nonDefaultActiveSelfSubscriptions.size)
            assertEquals(7, result.nonDefaultActiveSelfSubscriptions.single().subId)
            assertEquals(
                "Carrier B",
                result.nonDefaultActiveSelfSubscriptions.single().subscriptionName,
            )
        }
    }

    @Test
    fun getSubscriptionSettings_marksCellBroadcastDisabledWhenPackageLookupFails() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            every { defaultPhoneUtils.activeSubscriptionCount } returns 1
            every { defaultPhoneUtils.isDefaultSmsApp } returns true
            every {
                packageManager.getApplicationEnabledSetting(UIIntents.CMAS_COMPONENT)
            } throws IllegalArgumentException("missing")
            stubPerSubscriptionData(
                subId = ParticipantData.DEFAULT_SELF_SUB_ID,
            )

            val result = createRepository().getSubscriptionSettings()

            assertFalse(result.isCellBroadcastAppEnabled)
        }
    }

    @Test
    fun setSubscriptionBooleanPref_writesRequestedSubscriptionPref() {
        runTest(
            context = mainDispatcherRule.testDispatcher,
        ) {
            val prefs = mockk<BuglePrefs>()
            every { BuglePrefs.getSubscriptionPrefs(7) } returns prefs
            every {
                prefs.putBoolean(
                    DELIVERY_REPORTS_PREF_KEY,
                    true,
                )
            } just runs

            createRepository().setSubscriptionBooleanPref(
                subId = 7,
                pref = SubscriptionBooleanPref.DELIVERY_REPORTS,
                enabled = true,
            )

            verify(exactly = 1) {
                prefs.putBoolean(
                    DELIVERY_REPORTS_PREF_KEY,
                    true,
                )
            }
        }
    }

    private fun createRepository(): SubscriptionSettingsRepositoryImpl {
        return SubscriptionSettingsRepositoryImpl(
            context = context,
            contentResolver = contentResolver,
            subscriptionManager = subscriptionManager,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun stubPerSubscriptionData(
        subId: Int,
        savedPhoneNumber: String = "",
        defaultPhoneNumber: String = "",
        formattedSavedPhoneNumber: String? = null,
        formattedDefaultPhoneNumber: String? = null,
        isGroupMmsSupported: Boolean = false,
        isGroupMmsEnabled: Boolean = false,
        autoRetrieveMms: Boolean = false,
        autoRetrieveMmsWhenRoaming: Boolean = false,
        isDeliveryReportsSupported: Boolean = false,
        deliveryReportsEnabled: Boolean = false,
        showCellBroadcast: Boolean = false,
    ) {
        val prefs = mockk<BuglePrefs>()
        val phoneUtils = mockk<PhoneUtils>()
        val mmsConfig = mockk<MmsConfig>()
        every { factory.getSubscriptionPrefs(subId) } returns prefs
        every { PhoneUtils.get(subId) } returns phoneUtils
        every { MmsConfig.get(subId) } returns mmsConfig
        every {
            prefs.getString(MMS_PHONE_NUMBER_PREF_KEY, "")
        } returns savedPhoneNumber
        every { phoneUtils.getCanonicalForSelf(false) } returns defaultPhoneNumber
        if (savedPhoneNumber.isNotEmpty()) {
            every { phoneUtils.formatForDisplay(savedPhoneNumber) } returns
                formattedSavedPhoneNumber
        }
        if (defaultPhoneNumber.isNotEmpty()) {
            every {
                phoneUtils.formatForDisplay(defaultPhoneNumber)
            } returns formattedDefaultPhoneNumber
        }
        every { mmsConfig.groupMmsEnabled } returns isGroupMmsSupported
        every {
            prefs.getBoolean(
                GROUP_MMS_PREF_KEY,
                true,
            )
        } returns isGroupMmsEnabled
        every {
            prefs.getBoolean(
                AUTO_RETRIEVE_MMS_PREF_KEY,
                true,
            )
        } returns autoRetrieveMms
        every {
            prefs.getBoolean(
                AUTO_RETRIEVE_MMS_WHEN_ROAMING_PREF_KEY,
                false,
            )
        } returns autoRetrieveMmsWhenRoaming
        every { mmsConfig.smsDeliveryReportsEnabled } returns isDeliveryReportsSupported
        every {
            prefs.getBoolean(
                DELIVERY_REPORTS_PREF_KEY,
                false,
            )
        } returns deliveryReportsEnabled
        every { mmsConfig.showCellBroadcast } returns showCellBroadcast
    }

    private companion object {
        private const val MMS_PHONE_NUMBER_PREF_KEY = "mms_phone_number"
        private const val GROUP_MMS_PREF_KEY = "group_mms"
        private const val AUTO_RETRIEVE_MMS_PREF_KEY = "auto_retrieve_mms"
        private const val AUTO_RETRIEVE_MMS_WHEN_ROAMING_PREF_KEY =
            "auto_retrieve_mms_when_roaming"
        private const val DELIVERY_REPORTS_PREF_KEY = "delivery_reports"
    }
}
