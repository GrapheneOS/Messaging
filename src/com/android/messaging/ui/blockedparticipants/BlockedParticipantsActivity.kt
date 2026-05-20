package com.android.messaging.ui.blockedparticipants

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsEffectHandlerImpl
import com.android.messaging.ui.blockedparticipants.screen.BlockedParticipantsScreen
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlockedParticipantsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val effectHandler = BlockedParticipantsEffectHandlerImpl()

        setContent {
            AppTheme {
                BlockedParticipantsScreen(
                    effectHandler = effectHandler,
                    onNavigateBack = ::finish,
                )
            }
        }
    }
}
