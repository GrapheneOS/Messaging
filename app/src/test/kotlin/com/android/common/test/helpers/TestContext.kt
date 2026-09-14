package com.android.common.test.helpers

import android.content.Context
import org.robolectric.RuntimeEnvironment

internal val targetContext: Context
    get() {
        return RuntimeEnvironment.getApplication()
    }
