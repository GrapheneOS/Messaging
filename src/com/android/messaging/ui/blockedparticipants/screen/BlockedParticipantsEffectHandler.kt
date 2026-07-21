package com.android.messaging.ui.blockedparticipants.screen

import android.app.Activity
import android.graphics.Point
import android.net.Uri
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.blockedparticipants.screen.model.BlockedParticipantsScreenEffect as Effect
import com.android.messaging.util.ContactUtil
import com.android.messaging.util.UiUtils

@Composable
internal fun rememberBlockedParticipantsEffectHandler(): BlockedParticipantsEffectHandler {
    val activity = checkNotNull(LocalActivity.current)
    val hostView = LocalView.current

    return remember(activity, hostView) {
        BlockedParticipantsEffectHandlerImpl(
            activity = activity,
            hostView = hostView,
        )
    }
}

internal interface BlockedParticipantsEffectHandler {
    fun handle(effect: Effect)
}

internal class BlockedParticipantsEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
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

            is Effect.ShowOrAddContact -> {
                ContactUtil.showOrAddContact(
                    hostView,
                    effect.contactId,
                    effect.contactLookupKey,
                    effect.avatarUri?.let(Uri::parse),
                    effect.normalizedDestination,
                )
            }
        }
    }
}
