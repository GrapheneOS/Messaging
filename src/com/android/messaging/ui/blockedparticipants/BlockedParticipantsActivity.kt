package com.android.messaging.ui.blockedparticipants

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.BugleComponentActivity
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsEffectHandlerImpl
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsScreen
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedParticipantsActivity : BugleComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFinishing) {
            return
        }

        enableEdgeToEdge()

        setContent {
            AppTheme {
                val hostView = LocalView.current
                val effectHandler = remember(hostView) {
                    BlockedParticipantsEffectHandlerImpl(
                        activity = this,
                        hostView = hostView,
                    )
                }

                BlockedParticipantsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
