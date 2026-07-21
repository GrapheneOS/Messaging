package com.android.messaging.data.sms

import android.content.Context
import android.content.pm.PackageManager
import com.android.messaging.util.OsUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SmsReceiverToggleImplTest {

    private lateinit var packageManager: PackageManager
    private lateinit var context: Context

    private val toggle = SmsReceiverToggleImpl()

    @Before
    fun setUp() {
        packageManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.packageName } returns PACKAGE_NAME
        every { context.packageManager } returns packageManager
        mockkStatic(OsUtil::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun update_whenSecondaryUser_enablesSmsReceiver() {
        every { OsUtil.isSecondaryUser() } returns true

        toggle.update(context)

        verifyReceiverState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    @Test
    fun update_whenPrimaryUser_disablesSmsReceiver() {
        every { OsUtil.isSecondaryUser() } returns false

        toggle.update(context)

        verifyReceiverState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }

    private fun verifyReceiverState(expectedState: Int) {
        verify {
            packageManager.setComponentEnabledSetting(
                match { componentName ->
                    componentName.packageName == PACKAGE_NAME &&
                        componentName.className == SMS_RECEIVER_CLASS
                },
                expectedState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private companion object {
        private const val PACKAGE_NAME = "com.android.messaging"
        private const val SMS_RECEIVER_CLASS = "com.android.messaging.receiver.SmsReceiver"
    }
}
