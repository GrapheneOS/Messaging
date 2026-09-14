package com.android.messaging.ui.classzero

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.classzero.model.ClassZeroScreenEffect as Effect
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.core.CollectEvents
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class ClassZeroActivity : ComponentActivity() {

    private val screenModel: ClassZeroScreenModel by viewModels<ClassZeroViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        screenModel.onInitialMessageReceived(
            messageValues = messageValuesFromIntent(intent = intent),
        )

        setContent {
            AppTheme {
                val uiState by screenModel.uiState.collectAsStateWithLifecycle()

                CollectEvents(events = screenModel.effects) { effect ->
                    when (effect) {
                        Effect.Finish -> finish()
                    }
                }

                uiState?.let { state ->
                    ClassZeroScreen(
                        uiState = state,
                        onSaveClicked = screenModel::onSaveClicked,
                        onCancelClicked = screenModel::onCancelClicked,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        screenModel.onNewMessageReceived(
            messageValues = messageValuesFromIntent(intent = intent),
        )
    }

    override fun onStart() {
        super.onStart()
        screenModel.onHostStarted()
    }

    override fun onStop() {
        screenModel.onHostStopped()
        super.onStop()
    }

    private fun messageValuesFromIntent(intent: Intent): ContentValues? {
        return intent.getParcelableExtra(
            UIIntents.UI_INTENT_EXTRA_MESSAGE_VALUES,
            ContentValues::class.java,
        )
    }
}
