package com.android.messaging.util

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import com.android.messaging.FactoryTestAccess
import com.android.messaging.testutil.installTestFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class OsUtilRequiredPermissionsTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        installTestFactory(context = application)
        clearCachedPermissionStates()
        shadowOf(application.getSystemService(RoleManager::class.java))
            .addHeldRole(RoleManager.ROLE_SMS)
    }

    @After
    fun tearDown() {
        clearCachedPermissionStates()
        FactoryTestAccess.reset()
    }

    @Test
    fun readSmsRegrantedAlone_restOfTheSmsGroupIsStillRequired() {
        shadowOf(application).grantPermissions(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
        )

        assertFalse(
            "without RECEIVE_SMS the system drops incoming messages, so the app must not" +
                " count as ready to run",
            OsUtil.hasRequiredPermissions(),
        )
        assertEquals(
            SMS_ROLE_PERMISSIONS_BESIDES_READ_SMS.toSet(),
            OsUtil.getMissingRequiredPermissions().toSet(),
        )
    }

    @Test
    fun wholeSmsGroupGranted_requiredPermissionsAreHeld() {
        shadowOf(application).grantPermissions(
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            *SMS_ROLE_PERMISSIONS_BESIDES_READ_SMS.toTypedArray(),
        )

        assertTrue(OsUtil.hasRequiredPermissions())
        assertEquals(0, OsUtil.getMissingRequiredPermissions().size)
    }

    private fun clearCachedPermissionStates() {
        val cacheField = OsUtil::class.java.getDeclaredField(PERMISSION_CACHE_FIELD)
        cacheField.isAccessible = true
        (cacheField.get(null) as MutableMap<*, *>).clear()
    }

    private companion object {
        private const val PERMISSION_CACHE_FIELD = "sPermissions"

        private val SMS_ROLE_PERMISSIONS_BESIDES_READ_SMS = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.SEND_SMS,
            "android.permission.READ_CELL_BROADCASTS",
        )
    }
}
