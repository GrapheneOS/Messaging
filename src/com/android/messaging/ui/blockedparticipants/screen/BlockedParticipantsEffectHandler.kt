package com.android.messaging.ui.blockedparticipants.screen

import android.app.Activity
import android.graphics.Point
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect as Effect
import com.android.messaging.ui.contact.model.AddContactRequest
import com.android.messaging.ui.contact.showContactCard
import com.android.messaging.util.UiUtils

@Composable
internal fun rememberBlockedParticipantsEffectHandler(
    onNavigateToAddContact: (AddContactRequest) -> Unit,
): BlockedParticipantsEffectHandler {
    val activity = checkNotNull(LocalActivity.current)
    val hostView = LocalView.current
    val currentOnNavigateToAddContact = rememberUpdatedState(newValue = onNavigateToAddContact)

    return remember(activity, hostView) {
        BlockedParticipantsEffectHandlerImpl(
            activity = activity,
            hostView = hostView,
            onNavigateToAddContact = { request ->
                currentOnNavigateToAddContact.value(request)
            },
        )
    }
}

internal interface BlockedParticipantsEffectHandler {
    fun handle(effect: Effect)
}

internal class BlockedParticipantsEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
    private val onNavigateToAddContact: (AddContactRequest) -> Unit,
) : BlockedParticipantsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.ShowMessage -> {
                UiUtils.showToastAtBottom(effect.messageResId)
            }

            is Effect.PlacePhoneCall -> {
                UIIntents.get().launchPhoneCallActivity(
                    activity,
                    effect.destination,
                    Point(0, 0),
                )
            }

            is Effect.ShowContactCard -> {
                showContactCard(
                    hostView = hostView,
                    contactId = effect.contactId,
                    contactLookupKey = effect.contactLookupKey,
                )
            }

            is Effect.AddContact -> {
                onNavigateToAddContact(effect.request)
            }
        }
    }
}
