package com.android.messaging.domain.conversation.usecase.action

import android.app.role.RoleManager
import com.android.messaging.util.PhoneUtils
import javax.inject.Inject

internal fun interface CheckConversationActionRequirements {
    operator fun invoke(requiresPreferredSmsSim: Boolean): ConversationActionRequirementsResult
}

internal class CheckConversationActionRequirementsImpl @Inject constructor(
    private val roleManager: RoleManager,
) : CheckConversationActionRequirements {

    private val phoneUtils by lazy { PhoneUtils.getDefault() }

    override operator fun invoke(
        requiresPreferredSmsSim: Boolean,
    ): ConversationActionRequirementsResult {
        return when {
            !phoneUtils.isSmsCapable -> ConversationActionRequirementsResult.SmsNotCapable

            requiresPreferredSmsSim && !phoneUtils.hasPreferredSmsSim -> {
                ConversationActionRequirementsResult.NoPreferredSmsSim
            }

            !hasDefaultSmsRole() -> ConversationActionRequirementsResult.MissingDefaultSmsRole

            else -> ConversationActionRequirementsResult.Ready
        }
    }

    private fun hasDefaultSmsRole(): Boolean {
        return roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    }
}
