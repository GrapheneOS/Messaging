package com.android.messaging.ui.contact

import android.provider.ContactsContract
import android.view.View

internal fun showContactCard(
    hostView: View,
    contactId: Long,
    contactLookupKey: String,
) {
    val lookupUri = ContactsContract.Contacts.getLookupUri(contactId, contactLookupKey)

    ContactsContract.QuickContact.showQuickContact(
        hostView.context,
        hostView,
        lookupUri,
        ContactsContract.QuickContact.MODE_LARGE,
        null,
    )
}
