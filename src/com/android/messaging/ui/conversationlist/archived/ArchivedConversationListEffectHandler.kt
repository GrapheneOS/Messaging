package com.android.messaging.ui.conversationlist.archived

import android.app.Activity
import android.graphics.Point
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.net.toUri
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationlist.archived.model.ArchivedConversationListEffect as Effect
import com.android.messaging.util.ContactUtil
import com.android.messaging.util.DebugUtils

@Composable
internal fun rememberArchivedConversationListEffectHandler():
    ArchivedConversationListEffectHandler {
    val activity = checkNotNull(LocalActivity.current)
    val hostView = LocalView.current

    return remember(activity, hostView) {
        ArchivedConversationListEffectHandlerImpl(
            activity = activity,
            hostView = hostView,
        )
    }
}

internal interface ArchivedConversationListEffectHandler {
    fun handle(effect: Effect)
}

internal class ArchivedConversationListEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
) : ArchivedConversationListEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.PlaceCall -> {
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
                    effect.lookupKey,
                    effect.avatarUri?.toUri(),
                    effect.destination,
                )
            }

            Effect.OpenDebugOptions -> {
                DebugUtils.showDebugOptions(activity)
            }

            else -> Unit
        }
    }
}
