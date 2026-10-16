package com.android.messaging.data.appsettings.repository

import android.content.Context
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.AppBooleanPref
import com.android.messaging.data.appsettings.model.AppSettings
import com.android.messaging.data.appsettings.model.ConversationSwipeOption
import com.android.messaging.data.appsettings.model.ConversationSwipePref
import com.android.messaging.data.appsettings.model.ConversationSwipeSettings
import com.android.messaging.data.debug.DebugFeaturesProvider
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface AppSettingsRepository {
    suspend fun getAppSettings(): AppSettings
    suspend fun isYouTubeLinkPreviewsEnabled(): Boolean
    suspend fun getConversationSwipeSettings(): ConversationSwipeSettings
    suspend fun setBooleanPref(pref: AppBooleanPref, enabled: Boolean)
    suspend fun setConversationSwipeOption(
        pref: ConversationSwipePref,
        option: ConversationSwipeOption,
    )
}

internal class AppSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val debugFeaturesProvider: DebugFeaturesProvider,
) : AppSettingsRepository {

    override suspend fun getAppSettings(): AppSettings {
        return withContext(ioDispatcher) {
            val appPrefs = BuglePrefs.getApplicationPrefs()
            val phoneUtils = PhoneUtils.getDefault()
            val resources = context.resources

            AppSettings(
                isDefaultSmsApp = phoneUtils.isDefaultSmsApp,
                defaultSmsAppLabel = phoneUtils.defaultSmsAppLabel,
                sendSoundEnabled = appPrefs.getBoolean(
                    context.getString(R.string.send_sound_pref_key),
                    resources.getBoolean(R.bool.send_sound_pref_default),
                ),
                inConversationSoundEnabled = appPrefs.getBoolean(
                    context.getString(R.string.in_conversation_sound_pref_key),
                    resources.getBoolean(R.bool.in_conversation_sound_pref_default),
                ),
                youTubeLinkPreviewsEnabled = readYouTubeLinkPreviewsEnabled(),
                conversationSwipeSettings = readConversationSwipeSettings(),
                isDebugEnabled = debugFeaturesProvider.isEnabled(),
                dumpSmsEnabled = appPrefs.getBoolean(
                    context.getString(R.string.dump_sms_pref_key),
                    resources.getBoolean(R.bool.dump_sms_pref_default),
                ),
                dumpMmsEnabled = appPrefs.getBoolean(
                    context.getString(R.string.dump_mms_pref_key),
                    resources.getBoolean(R.bool.dump_mms_pref_default),
                ),
            )
        }
    }

    override suspend fun isYouTubeLinkPreviewsEnabled(): Boolean {
        return withContext(ioDispatcher) {
            readYouTubeLinkPreviewsEnabled()
        }
    }

    override suspend fun getConversationSwipeSettings(): ConversationSwipeSettings {
        return withContext(ioDispatcher) {
            readConversationSwipeSettings()
        }
    }

    override suspend fun setBooleanPref(
        pref: AppBooleanPref,
        enabled: Boolean,
    ) {
        withContext(ioDispatcher) {
            BuglePrefs.getApplicationPrefs().putBoolean(
                context.getString(pref.keyResId),
                enabled,
            )
        }
    }

    override suspend fun setConversationSwipeOption(
        pref: ConversationSwipePref,
        option: ConversationSwipeOption,
    ) {
        withContext(ioDispatcher) {
            BuglePrefs.getApplicationPrefs().putString(
                context.getString(pref.keyResId),
                option.name,
            )
        }
    }

    private fun readYouTubeLinkPreviewsEnabled(): Boolean {
        return BuglePrefs.getApplicationPrefs().getBoolean(
            context.getString(R.string.youtube_link_previews_pref_key),
            context.resources.getBoolean(R.bool.youtube_link_previews_pref_default),
        )
    }

    private fun readConversationSwipeSettings(): ConversationSwipeSettings {
        val default = ConversationSwipeSettings.Default

        return ConversationSwipeSettings(
            startToEnd = readConversationSwipeOption(ConversationSwipePref.START_TO_END)
                ?: default.startToEnd,
            endToStart = readConversationSwipeOption(ConversationSwipePref.END_TO_START)
                ?: default.endToStart,
        )
    }

    private fun readConversationSwipeOption(pref: ConversationSwipePref): ConversationSwipeOption? {
        val stored = BuglePrefs.getApplicationPrefs().getString(
            context.getString(pref.keyResId),
            null,
        )
        return ConversationSwipeOption.entries.firstOrNull { it.name == stored }
    }
}
