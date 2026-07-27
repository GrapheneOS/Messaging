package com.android.messaging.ui.conversationsettings.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Point
import android.provider.Settings
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.android.messaging.di.conversationsettings.ConversationSettingsEntryPoint
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.contact.model.AddContactRequest
import com.android.messaging.ui.contact.showContactCard
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect as Effect
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.util.UiUtils
import dagger.hilt.android.EntryPointAccessors

@Composable
internal fun rememberConversationSettingsEffectHandler(
    onNavigateToAddContact: (AddContactRequest) -> Unit,
): ConversationSettingsEffectHandler {
    val activity = checkNotNull(LocalActivity.current)
    val hostView = LocalView.current
    val context = LocalContext.current.applicationContext
    val currentOnNavigateToAddContact = rememberUpdatedState(newValue = onNavigateToAddContact)

    return remember(activity, hostView, context) {
        ConversationSettingsEffectHandlerImpl(
            activity = activity,
            hostView = hostView,
            clipboardManager = EntryPointAccessors
                .fromApplication(context, ConversationSettingsEntryPoint::class.java)
                .clipboardManager(),
            onNavigateToAddContact = { request ->
                currentOnNavigateToAddContact.value(request)
            },
        )
    }
}

internal interface ConversationSettingsEffectHandler {
    fun handle(effect: Effect)
}

internal class ConversationSettingsEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
    private val clipboardManager: ClipboardManager,
    private val onNavigateToAddContact: (AddContactRequest) -> Unit,
) : ConversationSettingsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenNotificationChannelSettings -> {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationChannelUtil.INCOMING_MESSAGES)
                    .putExtra(Settings.EXTRA_CONVERSATION_ID, effect.conversationId.value)

                NotificationChannelUtil.createConversationChannelForRuntime(
                    conversationId = effect.conversationId.value,
                    conversationTitle = effect.conversationTitle,
                )

                activity.startActivity(intent)
            }

            is Effect.CopyToClipboard -> {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, effect.text))
            }

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
