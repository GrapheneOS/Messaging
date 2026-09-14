package com.android.messaging.domain.contacts.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IsReadContactsPermissionGrantedImplTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        unmockkAll()
        mockkStatic(ContextCompat::class)
        context = mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun invoke_returnsTrueWhenPermissionIsGranted() {
        every {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            )
        } returns PackageManager.PERMISSION_GRANTED
        val useCase = createUseCase()

        val isGranted = useCase.invoke()

        assertEquals(true, isGranted)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            )
        }
    }

    @Test
    fun invoke_returnsFalseWhenPermissionIsDenied() {
        every {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            )
        } returns PackageManager.PERMISSION_DENIED
        val useCase = createUseCase()

        val isGranted = useCase.invoke()

        assertEquals(false, isGranted)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            )
        }
    }

    private fun createUseCase(): IsReadContactsPermissionGrantedImpl {
        return IsReadContactsPermissionGrantedImpl(
            context = context,
        )
    }
}
