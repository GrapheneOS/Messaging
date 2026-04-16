package com.android.messaging.domain.contacts.usecase

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface IsReadContactsPermissionGranted {
    operator fun invoke(): Boolean
}

internal class IsReadContactsPermissionGrantedImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : IsReadContactsPermissionGranted {

    override fun invoke(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
