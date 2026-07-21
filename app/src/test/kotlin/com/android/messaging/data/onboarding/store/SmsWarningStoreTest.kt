package com.android.messaging.data.onboarding.store

import com.android.messaging.FactoryTestAccess
import com.android.messaging.testutil.FakeBuglePrefs
import com.android.messaging.testutil.installTestFactory
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmsWarningStoreTest {

    @Before
    fun setUp() {
        installTestFactory(
            context = mockk(relaxed = true),
            prefs = FakeBuglePrefs(),
        )
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun isAcknowledged_whenNothingStored_returnsFalse() {
        assertFalse(createStore(versionCode = 42).isAcknowledged())
    }

    @Test
    fun isAcknowledged_afterAcknowledgeOnSameBuild_returnsTrue() {
        createStore(versionCode = 42).acknowledge()
        assertTrue(createStore(versionCode = 42).isAcknowledged())
    }

    @Test
    fun isAcknowledged_afterAcknowledgeOnEarlierBuild_returnsFalse() {
        createStore(versionCode = 42).acknowledge()
        assertFalse(createStore(versionCode = 43).isAcknowledged())
    }

    private fun createStore(versionCode: Int): SmsWarningStore {
        return SmsWarningStoreImpl(
            appVersionProvider = { versionCode },
        )
    }
}
