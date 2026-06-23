package com.android.messaging.ui.classzero

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.messaging.di.core.MainDispatcher
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.classzero.model.ClassZeroScreenEffect
import com.android.messaging.ui.core.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClassZeroActivity : ComponentActivity() {

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private val screenModel: ClassZeroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectScreenModel()
        screenModel.onInitialMessageReceived(
            messageValues = messageValuesFromIntent(intent = intent)
        )

        setContent {
            AppTheme {
                val state by screenModel.uiState.collectAsStateWithLifecycle()
                val uiState = state
                if (uiState != null) {
                    ClassZeroScreen(
                        uiState = uiState,
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
            messageValues = messageValuesFromIntent(
                intent = intent,
            ),
        )
    }

    private fun collectScreenModel() {
        lifecycleScope.launch(mainDispatcher) {
            repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                launch(mainDispatcher) {
                    screenModel.effects.collect { effect ->
                        handleEffect(effect = effect)
                    }
                }
            }
        }
    }

    private fun handleEffect(effect: ClassZeroScreenEffect) {
        when (effect) {
            ClassZeroScreenEffect.Finish -> {
                finish()
            }
        }
    }

    private fun messageValuesFromIntent(intent: Intent): ContentValues? {
        return intent.getParcelableExtra(
            UIIntents.UI_INTENT_EXTRA_MESSAGE_VALUES,
            ContentValues::class.java,
        )
    }
}
