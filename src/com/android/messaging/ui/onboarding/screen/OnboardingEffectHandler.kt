package com.android.messaging.ui.onboarding.screen

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.android.messaging.Factory
import com.android.messaging.di.onboarding.OnboardingEntryPoint
import com.android.messaging.ui.onboarding.screen.model.OnboardingScreenEffect as Effect
import dagger.hilt.android.EntryPointAccessors

@Composable
internal fun rememberOnboardingEffectHandler(activity: Activity): OnboardingEffectHandler {
    val context = LocalContext.current.applicationContext

    return remember(activity, context) {
        OnboardingEffectHandlerImpl(
            activity = activity,
            roleManager = EntryPointAccessors
                .fromApplication(context, OnboardingEntryPoint::class.java)
                .roleManager(),
        )
    }
}

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
