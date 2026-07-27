package com.android.messaging.ui.conversationlist.chats

import android.app.Activity
import android.graphics.Point
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.contact.model.AddContactRequest
import com.android.messaging.ui.contact.showContactCard
import com.android.messaging.ui.conversationlist.chats.model.ConversationListEffect as Effect
import com.android.messaging.util.DebugUtils

@Composable
internal fun rememberConversationListEffectHandler(
    onNavigateToAddContact: (AddContactRequest) -> Unit,
): ConversationListEffectHandler {
    val activity = checkNotNull(LocalActivity.current)
    val hostView = LocalView.current
    val currentOnNavigateToAddContact = rememberUpdatedState(newValue = onNavigateToAddContact)

    return remember(activity, hostView) {
        ConversationListEffectHandlerImpl(
            activity = activity,
            hostView = hostView,
            onNavigateToAddContact = { request ->
                currentOnNavigateToAddContact.value(request)
            },
        )
    }
}

internal interface ConversationListEffectHandler {
    fun handle(effect: Effect)
}

internal class ConversationListEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
    private val onNavigateToAddContact: (AddContactRequest) -> Unit,
) : ConversationListEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            Effect.OpenDebugOptions -> {
                DebugUtils.showDebugOptions(activity)
            }

            is Effect.PlaceCall -> {
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

            else -> Unit
        }
    }
}
