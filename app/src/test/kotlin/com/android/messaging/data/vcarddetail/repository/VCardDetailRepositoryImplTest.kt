package com.android.messaging.data.vcarddetail.repository

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.vcarddetail.mapper.VCardDetailMapper
import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.data.PersonItemData
import com.android.messaging.datamodel.data.VCardContactItemData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class VCardDetailRepositoryImplTest {

    private val context = mockk<Context>(relaxed = true)
    private val dataModel = mockk<DataModel>()
    private val mapper = mockk<VCardDetailMapper>()
    private val vCardData = mockk<VCardContactItemData>(relaxed = true)

    private val listenerSlot = slot<PersonItemData.PersonItemDataListener>()

    private lateinit var repository: VCardDetailRepositoryImpl

    private val contacts = persistentListOf(
        VCardContact(
            displayName = "Ada Lovelace",
            avatarUri = null,
            fields = persistentListOf(),
        ),
    )

    @Before
    fun setUp() {
        mockkStatic(DataModel::class)
        every { DataModel.get() } returns dataModel
        every { dataModel.createVCardContactItemData(any(), any<Uri>()) } returns vCardData
        every { vCardData.setListener(capture(listenerSlot)) } returns Unit
        every { vCardData.displayName } returns "Ada Lovelace"
        every { mapper.map(vCardData) } returns contacts

        repository = VCardDetailRepositoryImpl(
            context = context,
            vCardDetailMapper = mapper,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun observeVCard_blankUri_emitsFailedAndCompletes() = runTest {
        repository.observeVCard("   ").test {
            assertEquals(VCardDetailResult.Failed, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun observeVCard_onDataUpdated_emitsLoadingThenLoaded() = runTest {
        repository.observeVCard("content://vcard").test {
            assertEquals(VCardDetailResult.Loading, awaitItem())

            listenerSlot.captured.onPersonDataUpdated(vCardData)

            assertEquals(
                VCardDetailResult.Loaded(
                    contacts = contacts,
                    displayName = "Ada Lovelace",
                ),
                awaitItem(),
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun observeVCard_onDataFailed_emitsFailed() = runTest {
        repository.observeVCard("content://vcard").test {
            assertEquals(VCardDetailResult.Loading, awaitItem())

            listenerSlot.captured.onPersonDataFailed(vCardData, RuntimeException("boom"))

            assertEquals(VCardDetailResult.Failed, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun observeVCard_updateWithForeignData_isIgnored() = runTest {
        val foreignData = mockk<PersonItemData>()

        repository.observeVCard("content://vcard").test {
            assertEquals(VCardDetailResult.Loading, awaitItem())

            listenerSlot.captured.onPersonDataUpdated(foreignData)

            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun observeVCard_whenCancelled_unbindsData() = runTest {
        repository.observeVCard("content://vcard").test {
            assertEquals(VCardDetailResult.Loading, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        verify { vCardData.unbind(any()) }
    }
}
