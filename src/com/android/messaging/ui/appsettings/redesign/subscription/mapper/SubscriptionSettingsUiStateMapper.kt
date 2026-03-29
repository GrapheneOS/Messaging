package com.android.messaging.ui.appsettings.redesign.subscription.mapper

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.sms.MmsConfig
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.appsettings.redesign.subscription.model.SubscriptionSettingsUiState
import com.android.messaging.util.PhoneUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface SubscriptionSettingsUiStateMapper {
    fun isMultiSim(): Boolean
    fun mapSubscriptions(): List<SubscriptionSettingsUiState>
}

internal class SubscriptionSettingsUiStateMapperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
) : SubscriptionSettingsUiStateMapper {

    override fun isMultiSim(): Boolean {
        return PhoneUtils.getDefault().activeSubscriptionCount > 1
    }

    override fun mapSubscriptions(): List<SubscriptionSettingsUiState> {
        if (!isMultiSim()) {
            return listOf(
                mapSingleSubscription(
                    subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                    displayName = context.getString(R.string.advanced_settings),
                ),
            )
        }

        val selfParticipants = querySelfParticipants()
        val nonDefaultSelfs = selfParticipants.filter {
            !it.isDefaultSelf && it.isActiveSubscription
        }

        return when {
            nonDefaultSelfs.size > 1 -> nonDefaultSelfs.map { self ->
                mapSingleSubscription(
                    subId = self.subId,
                    displayName = context.getString(
                        R.string.sim_specific_settings,
                        self.subscriptionName,
                    ),
                )
            }

            nonDefaultSelfs.size == 1 -> listOf(
                mapSingleSubscription(
                    subId = nonDefaultSelfs.first().subId,
                    displayName = context.getString(R.string.advanced_settings),
                ),
            )

            else -> listOf(
                mapSingleSubscription(
                    subId = ParticipantData.DEFAULT_SELF_SUB_ID,
                    displayName = context.getString(R.string.advanced_settings),
                ),
            )
        }
    }

    private fun mapSingleSubscription(
        subId: Int,
        displayName: String,
    ): SubscriptionSettingsUiState {
        val subPrefs = Factory.get().getSubscriptionPrefs(subId)
        val phoneUtils = PhoneUtils.get(subId)
        val mmsConfig = MmsConfig.get(subId)

        val phoneNumberKey = context.getString(R.string.mms_phone_number_pref_key)
        val savedPhoneNumber = subPrefs.getString(phoneNumberKey, "")
        val defaultPhoneNumber = phoneUtils.getCanonicalForSelf(false)

        val displayPhoneNumber = when {
            !savedPhoneNumber.isNullOrEmpty() -> phoneUtils.formatForDisplay(savedPhoneNumber)
            !defaultPhoneNumber.isNullOrEmpty() -> phoneUtils.formatForDisplay(defaultPhoneNumber)
            else -> context.getString(R.string.unknown_phone_number_pref_display_value)
        }

        val isDefaultSmsApp = PhoneUtils.getDefault().isDefaultSmsApp
        val groupMmsPrefKey = context.getString(R.string.group_mms_pref_key)
        val autoRetrieveKey = context.getString(R.string.auto_retrieve_mms_pref_key)
        val autoRetrieveRoamingKey =
            context.getString(R.string.auto_retrieve_mms_when_roaming_pref_key)
        val deliveryReportsKey = context.getString(R.string.delivery_reports_pref_key)

        return SubscriptionSettingsUiState(
            subId = subId,
            displayName = displayName,
            displayDetail = displayPhoneNumber,
            phoneNumber = savedPhoneNumber.orEmpty(),
            defaultPhoneNumber = defaultPhoneNumber.orEmpty(),
            isGroupMmsSupported = mmsConfig.groupMmsEnabled,
            isGroupMmsEnabled = subPrefs.getBoolean(
                groupMmsPrefKey,
                context.resources.getBoolean(R.bool.group_mms_pref_default),
            ),
            autoRetrieveMms = subPrefs.getBoolean(
                autoRetrieveKey,
                context.resources.getBoolean(R.bool.auto_retrieve_mms_pref_default),
            ),
            autoRetrieveMmsWhenRoaming = subPrefs.getBoolean(
                autoRetrieveRoamingKey,
                context.resources.getBoolean(R.bool.auto_retrieve_mms_when_roaming_pref_default),
            ),
            isDeliveryReportsSupported = mmsConfig.smsDeliveryReportsEnabled,
            deliveryReportsEnabled = subPrefs.getBoolean(
                deliveryReportsKey,
                context.resources.getBoolean(R.bool.delivery_reports_pref_default),
            ),
            isWirelessAlertsSupported = mmsConfig.showCellBroadcast && isCellBroadcastAppEnabled(),
            isDefaultSmsApp = isDefaultSmsApp,
        )
    }

    private fun querySelfParticipants(): List<ParticipantData> {
        val cursor = contentResolver.query(
            MessagingContentProvider.PARTICIPANTS_URI,
            ParticipantData.ParticipantsQuery.PROJECTION,
            ParticipantColumns.SUB_ID + " <> ?",
            arrayOf(ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
            null,
        ) ?: return emptyList()

        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(ParticipantData.getFromCursor(it))
                }
            }
        }
    }

    private fun isCellBroadcastAppEnabled(): Boolean {
        return try {
            context.packageManager
                .getApplicationEnabledSetting(UIIntents.CMAS_COMPONENT) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
