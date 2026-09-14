package com.android.messaging.domain.conversation.usecase.action

import android.app.role.RoleManager
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckConversationActionRequirementsImplTest {

    private val phoneUtils = mockk<PhoneUtils>()
    private val roleManager = mockk<RoleManager>()

    @Before
    fun setUp() {
        mockkStatic(PhoneUtils::class)
        every { PhoneUtils.getDefault() } returns phoneUtils
    }

    @After
    fun tearDown() {
        unmockkStatic(PhoneUtils::class)
    }

    @Test
    fun invoke_whenDeviceIsNotSmsCapable_returnsSmsNotCapable() {
        every { phoneUtils.isSmsCapable } returns false
        val useCase = CheckConversationActionRequirementsImpl(roleManager = roleManager)

        assertEquals(
            ConversationActionRequirementsResult.SmsNotCapable,
            useCase(),
        )
    }

    @Test
    fun invoke_whenPreferredSmsSimIsMissing_returnsNoPreferredSmsSim() {
        every { phoneUtils.isSmsCapable } returns true
        every { phoneUtils.hasPreferredSmsSim } returns false
        val useCase = CheckConversationActionRequirementsImpl(roleManager = roleManager)

        assertEquals(
            ConversationActionRequirementsResult.NoPreferredSmsSim,
            useCase(),
        )
    }

    @Test
    fun invoke_whenSmsRoleIsUnavailable_returnsMissingDefaultSmsRole() {
        every { phoneUtils.isSmsCapable } returns true
        every { phoneUtils.hasPreferredSmsSim } returns true
        every {
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
        } returns false
        val useCase = CheckConversationActionRequirementsImpl(roleManager = roleManager)

        assertEquals(
            ConversationActionRequirementsResult.MissingDefaultSmsRole,
            useCase(),
        )
    }

    @Test
    fun invoke_whenSmsRoleIsNotHeld_returnsMissingDefaultSmsRole() {
        every { phoneUtils.isSmsCapable } returns true
        every { phoneUtils.hasPreferredSmsSim } returns true
        every {
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
        } returns true
        every {
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } returns false
        val useCase = CheckConversationActionRequirementsImpl(roleManager = roleManager)

        assertEquals(
            ConversationActionRequirementsResult.MissingDefaultSmsRole,
            useCase(),
        )
    }

    @Test
    fun invoke_whenAllRequirementsAreSatisfied_returnsReady() {
        every { phoneUtils.isSmsCapable } returns true
        every { phoneUtils.hasPreferredSmsSim } returns true
        every {
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
        } returns true
        every {
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } returns true
        val useCase = CheckConversationActionRequirementsImpl(roleManager = roleManager)

        assertEquals(
            ConversationActionRequirementsResult.Ready,
            useCase(),
        )
    }
}
