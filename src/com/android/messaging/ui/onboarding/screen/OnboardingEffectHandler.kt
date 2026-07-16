package com.android.messaging.ui.onboarding.screen

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.android.messaging.Factory
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.onboarding.screen.model.OnboardingScreenEffect as Effect

internal interface OnboardingEffectHandler {
    fun handle(effect: Effect)

    fun createSmsRoleIntent(): Intent
}

internal class OnboardingEffectHandlerImpl(
    private val activity: Activity,
    private val roleManager: RoleManager,
) : OnboardingEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            Effect.OpenAppSettings -> {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts(PACKAGE_SCHEME, activity.packageName, null),
                )
                activity.startActivity(intent)
            }

            Effect.Redirect -> {
                Factory.get().onRequiredPermissionsAcquired()
                UIIntents.get().launchConversationListActivity(activity)
                activity.finish()
            }

            is Effect.RequestRuntimePermissions -> Unit
            is Effect.RequestSmsRole -> Unit
        }
    }

    override fun createSmsRoleIntent(): Intent {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
    }

    private companion object {
        private const val PACKAGE_SCHEME = "package"
    }
}
