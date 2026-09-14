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
        assertFalse(SmsWarningStoreImpl().isAcknowledged())
    }

    @Test
    fun isAcknowledged_afterAcknowledge_returnsTrue() {
        SmsWarningStoreImpl().acknowledge()
        assertTrue(SmsWarningStoreImpl().isAcknowledged())
    }
}
