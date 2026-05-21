package com.android.messaging.ui.conversationsettings.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.provider.Settings
import android.view.View
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect as Effect
import com.android.messaging.util.ContactUtil
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.util.UiUtils

internal interface ConversationSettingsEffectHandler {
    fun handle(effect: Effect)
}

internal class ConversationSettingsEffectHandlerImpl(
    private val activity: Activity,
    private val hostView: View,
    private val clipboardManager: ClipboardManager,
) : ConversationSettingsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenNotificationChannelSettings -> {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, effect.conversationId)
                    .putExtra(Settings.EXTRA_CONVERSATION_ID, effect.conversationId)

                NotificationChannelUtil.createConversationChannel(
                    conversationId = effect.conversationId,
                    conversationTitle = effect.conversationTitle,
                    legacyNotificationsEnabled = effect.legacyPrefs.notificationsEnabled,
                    legacyRingtoneString = effect.legacyPrefs.ringtoneString,
                    legacyVibrationEnabled = effect.legacyPrefs.vibrationEnabled,
                )

                activity.startActivity(intent)
            }

            is Effect.OpenParticipantChat -> {
                val intent = UIIntents.get().getIntentForConversationActivity(
                    activity,
                    effect.conversationId,
                    null,
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
                    effect.phoneNumber,
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
