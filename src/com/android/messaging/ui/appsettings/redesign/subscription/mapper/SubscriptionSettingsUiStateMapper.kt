package com.android.messaging.ui.appsettings.redesign.subscription.mapper

import android.content.ContentResolver
import android.content.Context
import com.android.messaging.Factory
import com.android.messaging.R
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.ParticipantData
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

        val phoneNumberKey = context.getString(R.string.mms_phone_number_pref_key)
        val savedPhoneNumber = subPrefs.getString(phoneNumberKey, "")
        val defaultPhoneNumber = phoneUtils.getCanonicalForSelf(false)

        val displayPhoneNumber = when {
            !savedPhoneNumber.isNullOrEmpty() -> phoneUtils.formatForDisplay(savedPhoneNumber)
            !defaultPhoneNumber.isNullOrEmpty() -> phoneUtils.formatForDisplay(defaultPhoneNumber)
            else -> context.getString(R.string.unknown_phone_number_pref_display_value)
        }

        return SubscriptionSettingsUiState(
            subId = subId,
            displayName = displayName,
            displayDetail = displayPhoneNumber,
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
}
