package com.android.messaging.ui.contact

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.contact.model.AddContactUiState
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.util.AccessibilityUtil

class AddContactActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destination = intent.getStringExtra(EXTRA_DESTINATION)
        if (destination.isNullOrEmpty()) {
            finish()
            return
        }

        val uiState = AddContactUiState(
            avatarUri = intent.getStringExtra(EXTRA_AVATAR_URI),
            destination = destination,
            vocalizedDestination = AccessibilityUtil.getVocalizedPhoneNumber(
                resources,
                destination,
            ),
        )

        setContent {
            AppTheme {
                AddContactConfirmationDialog(
                    uiState = uiState,
                    onConfirm = { onConfirm(destination) },
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun onConfirm(destination: String) {
        UIIntents.get().launchAddContactActivity(this, destination)
        finish()
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_AVATAR_URI = "avatar_uri"
    }
}
