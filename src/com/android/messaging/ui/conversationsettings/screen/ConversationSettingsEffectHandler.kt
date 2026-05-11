package com.android.messaging.ui.conversationsettings.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import com.android.messaging.ui.conversation.ConversationActivity
import com.android.messaging.util.NotificationChannelUtil
import com.android.messaging.ui.conversationsettings.screen.model.ConversationSettingsScreenEffect as Effect

internal interface ConversationSettingsEffectHandler {
    fun handle(effect: Effect)
}

internal class ConversationSettingsEffectHandlerImpl(
    private val activity: Activity,
    private val clipboardManager: ClipboardManager,
) : ConversationSettingsEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenNotificationChannelSettings -> {
                NotificationChannelUtil.createConversationChannel(
                    effect.conversationId,
                    effect.conversationTitle,
                    effect.legacyNotificationEnabled,
                    effect.legacyRingtoneString,
                    effect.legacyVibrationEnabled,
                )
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, effect.conversationId)
                    .putExtra(Settings.EXTRA_CONVERSATION_ID, effect.conversationId)
                activity.startActivity(intent)
            }

            is Effect.FinishAfterBlock -> {
                activity.setResult(ConversationActivity.FINISH_RESULT_CODE)
                activity.finish()
            }

            is Effect.CopyToClipboard -> {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, effect.text))
            }
        }
    }
}
