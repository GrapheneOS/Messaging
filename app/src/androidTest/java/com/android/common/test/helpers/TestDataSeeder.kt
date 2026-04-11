package com.android.common.test.helpers

import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.debug.clearSeededTestData
import com.android.messaging.debug.seedTestData

object TestDataSeeder {

    fun seedTestData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        seedTestData(context)
    }

    fun clearSeededTestData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clearSeededTestData(context)
    }
}
