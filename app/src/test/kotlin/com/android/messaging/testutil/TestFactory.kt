package com.android.messaging.testutil

import android.content.Context
import android.database.Cursor
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.datamodel.DataModel
import com.android.messaging.util.BugleGservices
import com.android.messaging.util.BuglePrefs
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk

internal fun installTestFactory(
    context: Context,
    dataModel: DataModel = mockk(relaxed = true),
    prefs: BuglePrefs = FakeBuglePrefs(),
    gServices: BugleGservices = createDefaultTestGservices(),
    phoneUtils: PhoneUtils = createDefaultTestPhoneUtils(),
) {
    FactoryTestAccess.install(
        createTestFactory(
            context = context,
            dataModel = dataModel,
            prefs = prefs,
            gServices = gServices,
            phoneUtils = phoneUtils,
        ),
    )
}

internal fun createTestFactory(
    context: Context,
    dataModel: DataModel,
    prefs: BuglePrefs,
    gServices: BugleGservices = createDefaultTestGservices(),
    phoneUtils: PhoneUtils = createDefaultTestPhoneUtils(),
): Factory {
    return mockk<Factory>(relaxed = true).also { factory ->
        every { factory.getApplicationContext() } returns context
        every { factory.getDataModel() } returns dataModel
        every { factory.getBugleGservices() } returns gServices
        every { factory.getApplicationPrefs() } returns prefs
        every { factory.getSubscriptionPrefs(any()) } returns prefs
        every { factory.getWidgetPrefs() } returns prefs
        every { factory.getPhoneUtils(any()) } returns phoneUtils
    }
}

private fun createDefaultTestGservices(): BugleGservices {
    val gServices = mockk<BugleGservices>(relaxed = true)
    every { gServices.getLong(any(), any()) } answers { secondArg() }
    every { gServices.getInt(any(), any()) } answers { secondArg() }
    every { gServices.getBoolean(any(), any()) } answers { secondArg() }
    every { gServices.getString(any(), any()) } answers { secondArg() }
    every { gServices.getFloat(any(), any()) } answers { secondArg() }
    return gServices
}

private fun createDefaultTestPhoneUtils(): PhoneUtils {
    val phoneUtils = mockk<PhoneUtils>(relaxed = true)
    every { phoneUtils.getSubIdFromTelephony(any(), any()) } answers {
        firstArg<Cursor>().getInt(secondArg<Int>())
    }
    every { phoneUtils.getCanonicalBySimLocale(any()) } answers { firstArg() }
    every { phoneUtils.formatForDisplay(any()) } answers { firstArg() }
    return phoneUtils
}
