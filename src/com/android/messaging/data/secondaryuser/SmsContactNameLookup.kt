package com.android.messaging.data.secondaryuser

import android.content.Context
import com.android.messaging.util.ContactUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface SmsContactNameLookup {
    fun lookup(address: String): String?
}

internal class SmsContactNameLookupImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : SmsContactNameLookup {

    override fun lookup(address: String): String? {
        return ContactUtil.lookupDestination(context, address)
            .performSynchronousQuery()
            ?.use { cursor ->
                when {
                    cursor.moveToFirst() -> cursor.getString(ContactUtil.INDEX_DISPLAY_NAME)
                    else -> null
                }
            }
    }
}
