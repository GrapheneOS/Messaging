package com.android.messaging.domain.conversation.usecase.action

import android.app.role.RoleManager
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateDefaultSmsRoleRequestImplTest {

    @Test
    fun invoke_whenSmsRoleIsUnavailable_returnsNull() {
        val roleManager = mockk<RoleManager>()
        every {
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
        } returns false

        val useCase = CreateDefaultSmsRoleRequestImpl(roleManager = roleManager)

        assertNull(useCase())
    }

    @Test
    fun invoke_whenSmsRoleIsAvailable_returnsRoleRequestIntent() {
        val requestIntent = Intent("request-sms-role")
        val roleManager = mockk<RoleManager>()

        every {
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
        } returns true

        every {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } returns requestIntent

        val useCase = CreateDefaultSmsRoleRequestImpl(roleManager = roleManager)

        assertSame(requestIntent, useCase())
    }
}
