package com.android.messaging.testutil

import android.database.MatrixCursor
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.data.ParticipantData

internal fun createParticipantsCursor(vararg rows: TestParticipantRow): MatrixCursor {
    val cursor = MatrixCursor(ParticipantData.ParticipantsQuery.PROJECTION)
    rows.forEach { row ->
        cursor.addRow(
            ParticipantData.ParticipantsQuery.PROJECTION.map { columnName ->
                row.toColumnValues()[columnName]
            }.toTypedArray(),
        )
    }
    return cursor
}

internal fun participantRow(
    participantId: String,
    subId: Int = ParticipantData.OTHER_THAN_SELF_SUB_ID,
    slotId: Int = 0,
    subscriptionName: String? = "",
    displayDestination: String = "",
    normalizedDestination: String = displayDestination,
    sendDestination: String = normalizedDestination,
    profilePhotoUri: String? = "",
    lookupKey: String = "",
    contactId: Long = 0L,
    fullName: String = "",
    firstName: String = "",
    subscriptionColor: Int = 0,
    contactDestination: String = normalizedDestination,
): TestParticipantRow {
    return TestParticipantRow(
        participantId = participantId,
        subId = subId,
        slotId = slotId,
        subscriptionName = subscriptionName,
        displayDestination = displayDestination,
        normalizedDestination = normalizedDestination,
        sendDestination = sendDestination,
        profilePhotoUri = profilePhotoUri,
        lookupKey = lookupKey,
        contactId = contactId,
        fullName = fullName,
        firstName = firstName,
        subscriptionColor = subscriptionColor,
        contactDestination = contactDestination,
    )
}

internal data class TestParticipantRow(
    val participantId: String,
    val subId: Int = ParticipantData.OTHER_THAN_SELF_SUB_ID,
    val slotId: Int = 0,
    val subscriptionName: String? = "",
    val displayDestination: String = "",
    val normalizedDestination: String = "",
    val sendDestination: String = normalizedDestination,
    val profilePhotoUri: String? = "",
    val lookupKey: String = "",
    val contactId: Long = 0L,
    val fullName: String = "",
    val firstName: String = "",
    val subscriptionColor: Int = 0,
    val contactDestination: String = normalizedDestination,
) {
    fun toColumnValues(): Map<String, Any?> {
        return mapOf(
            ParticipantColumns._ID to participantId,
            ParticipantColumns.SUB_ID to subId,
            ParticipantColumns.SIM_SLOT_ID to slotId,
            ParticipantColumns.NORMALIZED_DESTINATION to normalizedDestination,
            ParticipantColumns.SEND_DESTINATION to sendDestination,
            ParticipantColumns.DISPLAY_DESTINATION to displayDestination,
            ParticipantColumns.FULL_NAME to fullName,
            ParticipantColumns.FIRST_NAME to firstName,
            ParticipantColumns.PROFILE_PHOTO_URI to profilePhotoUri,
            ParticipantColumns.CONTACT_ID to contactId,
            ParticipantColumns.LOOKUP_KEY to lookupKey,
            ParticipantColumns.BLOCKED to 0,
            ParticipantColumns.SUBSCRIPTION_COLOR to subscriptionColor,
            ParticipantColumns.SUBSCRIPTION_NAME to subscriptionName,
            ParticipantColumns.CONTACT_DESTINATION to contactDestination,
        )
    }
}
