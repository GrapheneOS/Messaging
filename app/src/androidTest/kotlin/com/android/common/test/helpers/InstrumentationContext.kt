package com.android.common.test.helpers

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry

internal val targetContext: Context
    get() {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
