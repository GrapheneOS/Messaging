package com.android.messaging.ui.vcarddetail.screen

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.android.messaging.R
import com.android.messaging.data.vcarddetail.model.VCardContact
import com.android.messaging.data.vcarddetail.model.VCardDetailResult
import com.android.messaging.data.vcarddetail.model.VCardFieldAction
import com.android.messaging.data.vcarddetail.repository.VCardDetailRepository
import com.android.messaging.domain.vcarddetail.model.AddVCardToContactsResult
import com.android.messaging.domain.vcarddetail.usecase.AddVCardToContacts
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.UIIntents
import com.android.messaging.ui.vcarddetail.screen.mapper.VCardDetailUiStateMapperImpl
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailAction as Action
import com.android.messaging.ui.vcarddetail.screen.model.VCardDetailScreenEffect as Effect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class VCardDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<VCardDetailRepository>()
    private val addVCardToContacts = mockk<AddVCardToContacts>()
    private val uiStateMapper = VCardDetailUiStateMapperImpl()

    private val results = MutableSharedFlow<VCardDetailResult>(extraBufferCapacity = 8)

    private val contacts = persistentListOf(
        VCardContact(
            displayName = "Ada Lovelace",
            avatarUri = null,
            fields = persistentListOf(),
        ),
    )

    @Test
    fun onLoaded_updatesUiStateAndAllowsAddToContacts() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            results.emit(
                VCardDetailResult.Loaded(
                    contacts = contacts,
                    displayName = "Ada Lovelace",
                ),
            )
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertFalse(uiState.isLoading)
            assertEquals(contacts, uiState.contacts)
            assertTrue(uiState.canAddToContacts)
        }

    @Test
    fun onFailed_emitsShowMessageAndClose() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            results.emit(VCardDetailResult.Failed)

            assertEquals(Effect.ShowMessage(R.string.failed_loading_vcard), awaitItem())
            assertEquals(Effect.Close, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fieldClicked_actionableField_emitsOpenFieldAction() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.FieldClicked(VCardFieldAction.Dial("+15550001")))

                assertEquals(
                    Effect.OpenFieldAction(VCardFieldAction.Dial("+15550001")),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun fieldClicked_noneAction_isIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onAction(Action.FieldClicked(VCardFieldAction.None))

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addToContactsClicked_whenPrepared_emitsLaunchWithDisplayName() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { addVCardToContacts(VCARD_URI, "Ada Lovelace") } returns
                AddVCardToContactsResult.Prepared(SCRATCH_URI)

            val viewModel = createViewModel()
            advanceUntilIdle()

            results.emit(
                VCardDetailResult.Loaded(
                    contacts = contacts,
                    displayName = "Ada Lovelace",
                ),
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onAction(Action.AddToContactsClicked)

                assertEquals(Effect.LaunchSaveToContacts(SCRATCH_URI), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun addToContactsClicked_secondTime_reusesCachedScratchUri() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { addVCardToContacts(any(), any()) } returns
                AddVCardToContactsResult.Prepared(SCRATCH_URI)

            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.AddToContactsClicked)
                assertEquals(Effect.LaunchSaveToContacts(SCRATCH_URI), awaitItem())

                viewModel.onAction(Action.AddToContactsClicked)
                assertEquals(Effect.LaunchSaveToContacts(SCRATCH_URI), awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 1) { addVCardToContacts(any(), any()) }
        }

    @Test
    fun addToContactsClicked_whenFailed_emitsShowMessage() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { addVCardToContacts(any(), any()) } returns AddVCardToContactsResult.Failed

            val viewModel = createViewModel()

            viewModel.effects.test {
                viewModel.onAction(Action.AddToContactsClicked)

                assertEquals(
                    Effect.ShowMessage(R.string.failed_saving_vcard_to_contacts),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createViewModel(): VCardDetailViewModel {
        every { repository.observeVCard(VCARD_URI) } returns results

        val uri = mockk<Uri>()
        every { uri.toString() } returns VCARD_URI
        val savedStateHandle = SavedStateHandle(
            mapOf(UIIntents.UI_INTENT_EXTRA_VCARD_URI to uri),
        )

        return VCardDetailViewModel(
            savedStateHandle = savedStateHandle,
            repository = repository,
            uiStateMapper = uiStateMapper,
            addVCardToContacts = addVCardToContacts,
        )
    }

    private companion object {
        private const val VCARD_URI = "content://vcard"
        private const val SCRATCH_URI = "content://scratch/vcard"
    }
}
