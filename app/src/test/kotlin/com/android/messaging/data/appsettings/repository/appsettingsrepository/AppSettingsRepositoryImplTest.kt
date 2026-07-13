package com.android.messaging.data.appsettings.repository.appsettingsrepository

import android.content.Context
import android.content.res.Resources
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppBooleanPref
import com.android.messaging.data.appsettings.repository.AppSettingsRepositoryImpl
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.util.BugleGservices
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class AppSettingsRepositoryImplTest {

    private lateinit var factory: Factory
    private lateinit var appPrefs: BuglePrefs
    private lateinit var bugleGservices: BugleGservices
    private lateinit var context: Context
    private lateinit var debugFeaturesProvider: DebugFeaturesProvider
    private lateinit var phoneUtils: PhoneUtils
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        factory = mockk()
        appPrefs = mockk()
        bugleGservices = mockk()
        context = mockk()
        debugFeaturesProvider = mockk()
        phoneUtils = mockk()
        resources = mockk()

        mockkStatic(Factory::class)

        every { Factory.get() } returns factory
        every { factory.applicationPrefs } returns appPrefs
        every { factory.bugleGservices } returns bugleGservices
        every { factory.getPhoneUtils(ParticipantData.DEFAULT_SELF_SUB_ID) } returns phoneUtils
        every { context.resources } returns resources
        every { context.getString(R.string.send_sound_pref_key) } returns SEND_SOUND_PREF_KEY
        every { context.getString(R.string.dump_sms_pref_key) } returns DUMP_SMS_PREF_KEY
        every { context.getString(R.string.dump_mms_pref_key) } returns DUMP_MMS_PREF_KEY
        every {
            context.getString(R.string.youtube_link_previews_pref_key)
        } returns YOUTUBE_LINK_PREVIEWS_PREF_KEY
        every {
            resources.getBoolean(R.bool.youtube_link_previews_pref_default)
        } returns YOUTUBE_LINK_PREVIEWS_DEFAULT
        every { resources.getBoolean(R.bool.send_sound_pref_default) } returns
            SEND_SOUND_DEFAULT
        every { resources.getBoolean(R.bool.dump_sms_pref_default) } returns DUMP_SMS_DEFAULT
        every { resources.getBoolean(R.bool.dump_mms_pref_default) } returns DUMP_MMS_DEFAULT
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getAppSettings_readsPhoneStateDebugStateAndBooleanPrefsWithResourceDefaults() {
        runTest {
            every { phoneUtils.isDefaultSmsApp } returns true
            every { phoneUtils.defaultSmsAppLabel } returns DEFAULT_SMS_APP_LABEL
            every { debugFeaturesProvider.isEnabled() } returns true
            every { appPrefs.getBoolean(SEND_SOUND_PREF_KEY, SEND_SOUND_DEFAULT) } returns false
            every { appPrefs.getBoolean(DUMP_SMS_PREF_KEY, DUMP_SMS_DEFAULT) } returns true
            every { appPrefs.getBoolean(DUMP_MMS_PREF_KEY, DUMP_MMS_DEFAULT) } returns false
            every {
                appPrefs.getBoolean(
                    YOUTUBE_LINK_PREVIEWS_PREF_KEY,
                    YOUTUBE_LINK_PREVIEWS_DEFAULT,
                )
            } returns true

            val result = createRepository(
                ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            ).getAppSettings()

            assertTrue(result.isDefaultSmsApp)
            assertEquals(DEFAULT_SMS_APP_LABEL, result.defaultSmsAppLabel)
            assertFalse(result.sendSoundEnabled)
            assertTrue(result.youTubeLinkPreviewsEnabled)
            assertTrue(result.isDebugEnabled)
            assertTrue(result.dumpSmsEnabled)
            assertFalse(result.dumpMmsEnabled)
            verify(exactly = 1) {
                appPrefs.getBoolean(SEND_SOUND_PREF_KEY, SEND_SOUND_DEFAULT)
                appPrefs.getBoolean(
                    YOUTUBE_LINK_PREVIEWS_PREF_KEY,
                    YOUTUBE_LINK_PREVIEWS_DEFAULT,
                )
                appPrefs.getBoolean(DUMP_SMS_PREF_KEY, DUMP_SMS_DEFAULT)
                appPrefs.getBoolean(DUMP_MMS_PREF_KEY, DUMP_MMS_DEFAULT)
            }
        }
    }

    @Test
    fun setBooleanPref_writesEveryApplicationBooleanPreferenceKey() {
        runTest {
            every { appPrefs.putBoolean(any(), any()) } just runs
            val repository = createRepository(
                ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            )

            repository.setBooleanPref(
                pref = AppBooleanPref.SEND_SOUND,
                enabled = true,
            )
            repository.setBooleanPref(
                pref = AppBooleanPref.DUMP_SMS,
                enabled = false,
            )
            repository.setBooleanPref(
                pref = AppBooleanPref.DUMP_MMS,
                enabled = true,
            )
            repository.setBooleanPref(
                pref = AppBooleanPref.YOUTUBE_LINK_PREVIEWS,
                enabled = true,
            )

            verify(exactly = 1) {
                appPrefs.putBoolean(
                    SEND_SOUND_PREF_KEY,
                    true,
                )
                appPrefs.putBoolean(
                    DUMP_SMS_PREF_KEY,
                    false,
                )
                appPrefs.putBoolean(
                    DUMP_MMS_PREF_KEY,
                    true,
                )
                appPrefs.putBoolean(
                    YOUTUBE_LINK_PREVIEWS_PREF_KEY,
                    true,
                )
            }
        }
    }

    @Test
    fun isYouTubeLinkPreviewsEnabled_readsPreferenceWithResourceDefault() {
        runTest {
            every {
                appPrefs.getBoolean(
                    YOUTUBE_LINK_PREVIEWS_PREF_KEY,
                    YOUTUBE_LINK_PREVIEWS_DEFAULT,
                )
            } returns true

            val result = createRepository(
                ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            ).isYouTubeLinkPreviewsEnabled()

            assertTrue(result)
        }
    }

    @Test
    fun isYouTubeLinkPreviewsEnabled_defaultsToDisabledFromResources() {
        runTest {
            every { appPrefs.getBoolean(any(), any()) } answers { secondArg() }

            val result = AppSettingsRepositoryImpl(
                context = RuntimeEnvironment.getApplication().applicationContext,
                ioDispatcher = UnconfinedTestDispatcher(testScheduler),
                debugFeaturesProvider = debugFeaturesProvider,
            ).isYouTubeLinkPreviewsEnabled()

            assertFalse(result)
        }
    }

    private fun createRepository(ioDispatcher: CoroutineDispatcher): AppSettingsRepositoryImpl {
        return AppSettingsRepositoryImpl(
            context = context,
            ioDispatcher = ioDispatcher,
            debugFeaturesProvider = debugFeaturesProvider,
        )
    }

    private companion object {
        private const val DEFAULT_SMS_APP_LABEL = "Messaging"
        private const val DUMP_MMS_DEFAULT = false
        private const val DUMP_MMS_PREF_KEY = "dump_mms"
        private const val DUMP_SMS_DEFAULT = true
        private const val DUMP_SMS_PREF_KEY = "dump_sms"
        private const val SEND_SOUND_DEFAULT = true
        private const val SEND_SOUND_PREF_KEY = "send_sound"
        private const val YOUTUBE_LINK_PREVIEWS_DEFAULT = false
        private const val YOUTUBE_LINK_PREVIEWS_PREF_KEY = "youtube_link_previews"
    }
}
