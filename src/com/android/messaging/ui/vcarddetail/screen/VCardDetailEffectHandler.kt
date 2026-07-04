package com.android.messaging.ui.vcarddetail.screen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailScreenEffect as Effect
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UiUtils

internal interface VCardDetailEffectHandler {
    fun handle(effect: Effect)
}

internal class VCardDetailEffectHandlerImpl(
    private val activity: Activity,
) : VCardDetailEffectHandler {

    override fun handle(effect: Effect) {
        when (effect) {
            is Effect.OpenFieldAction -> {
                openFieldAction(effect.action)
            }

            is Effect.LaunchSaveToContacts -> {
                launchSaveToContacts(effect.scratchUri)
            }

            is Effect.ShowMessage -> {
                UiUtils.showToastAtBottom(effect.messageResId)
            }

            Effect.Close -> {
                activity.finish()
            }
        }
    }

    private fun openFieldAction(action: VCardFieldAction) {
        val intent = fieldActionIntent(action) ?: return

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            LogUtil.w(LogUtil.BUGLE_TAG, "No activity found for vCard field action", e)
        }
    }

    private fun fieldActionIntent(action: VCardFieldAction): Intent? {
        return when (action) {
            is VCardFieldAction.Dial -> {
                Intent(Intent.ACTION_DIAL, "tel:${action.number}".toUri())
            }

            is VCardFieldAction.Email -> {
                Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(action.address))
                }
            }

            is VCardFieldAction.OpenMap -> {
                Intent(Intent.ACTION_VIEW, "geo:0,0?q=${Uri.encode(action.query)}".toUri())
            }

            is VCardFieldAction.OpenUrl -> {
                Intent(Intent.ACTION_VIEW, action.url.toUri())
            }

            VCardFieldAction.None -> null
        }
    }

    private fun launchSaveToContacts(scratchUri: String) {
        UIIntents.get().launchSaveVCardToContactsActivity(
            activity,
            scratchUri.toUri(),
        )
    }
}
