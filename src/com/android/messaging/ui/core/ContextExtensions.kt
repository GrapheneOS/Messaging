package com.android.messaging.ui.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window

internal fun Context.findActivityWindow(): Window? {
    return when (this) {
        is Activity -> window
        is ContextWrapper -> baseContext.findActivityWindow()
        else -> null
    }
}
