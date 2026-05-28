package com.android.messaging.ui.permissioncheck

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.permissioncheck.screen.PermissionCheckEffectHandlerImpl
import com.android.messaging.ui.permissioncheck.screen.PermissionCheckScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionCheckActivity : ComponentActivity() {

    @Inject
    lateinit var roleManager: RoleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val effectHandler = PermissionCheckEffectHandlerImpl(
            activity = this,
            roleManager = roleManager,
        )

        setContent {
            AppTheme {
                PermissionCheckScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
