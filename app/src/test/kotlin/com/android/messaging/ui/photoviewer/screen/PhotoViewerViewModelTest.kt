package com.android.messaging.ui.photoviewer.screen

import android.net.Uri
import app.cash.turbine.test
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.data.media.model.PhotoViewerItems
import com.android.messaging.data.media.model.PhotoViewerItemsLoadResult
import com.android.messaging.data.media.repository.PhotoViewerRepository
import com.android.messaging.domain.photoviewer.usecase.NormalizePhotoViewerUri
import com.android.messaging.domain.photoviewer.usecase.NormalizePhotoViewerUriImpl
import com.android.messaging.domain.photoviewer.usecase.PreparePhotoViewerSendUri
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerEffect
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerLoadState
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PhotoViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun firstEmission_usesInitialIndex() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1, photo2, photo3),
                    initialIndex = 1,
                ),
            )
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.currentPage)
            assertEquals(PhotoViewerLoadState.Loaded, viewModel.uiState.value.loadState)
        }
    }

    @Test
    fun refresh_whenCurrentItemRemains_preservesCurrentItem() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1, photo2, photo3),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            viewModel.onPageSettled(page = 2)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo3Refreshed, photo1, photo2),
                    initialIndex = 1,
                ),
            )
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.currentPage)
            assertEquals(photo3Refreshed.contentUri, viewModel.uiState.value.items[0].contentUri)
        }
    }

    @Test
    fun refresh_whenCurrentItemRemoved_clampsToNearestValidPage() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1, photo2, photo3),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            viewModel.onPageSettled(page = 2)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1, photo2),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.currentPage)
        }
    }

    @Test
    fun refresh_keepsDisplayMode() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            loadInitialItems(
                repositoryResults = repositoryResults,
                viewModel = viewModel,
            )

            viewModel.onEnterImmersiveMode()
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1, photo2),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            assertEquals(PhotoViewerDisplayMode.Immersive, viewModel.uiState.value.displayMode)
        }
    }

    @Test
    fun refresh_keepsMetadataSheetVisible() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            loadInitialItems(
                repositoryResults = repositoryResults,
                viewModel = viewModel,
            )

            viewModel.onMetadataClick()
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1, photo2),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.isMetadataSheetVisible)
        }
    }

    @Test
    fun refresh_withEmptyResult_hidesMetadataSheet() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            loadInitialItems(
                repositoryResults = repositoryResults,
                viewModel = viewModel,
            )

            viewModel.onMetadataClick()
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = emptyList(),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            assertEquals(PhotoViewerLoadState.Empty, viewModel.uiState.value.loadState)
            assertEquals(false, viewModel.uiState.value.isMetadataSheetVisible)
        }
    }

    @Test
    fun actions_emitEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onSaveClick()
                assertEquals(
                    PhotoViewerEffect.Save(
                        uri = photo1.contentUri,
                        contentType = IMAGE_JPEG,
                    ),
                    awaitItem(),
                )

                viewModel.onShareClick()
                assertEquals(
                    PhotoViewerEffect.Share(
                        uri = photo1.contentUri,
                        contentType = IMAGE_JPEG,
                    ),
                    awaitItem(),
                )

                viewModel.onForwardClick()
                assertEquals(
                    PhotoViewerEffect.Forward(
                        uri = photo1.contentUri,
                        contentType = IMAGE_JPEG,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun shareAndForward_whenPreparedUriIsDifferent_emitPreparedUri() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val preparedUri = Uri.parse("content://example/prepared/1")
            val viewModel = createViewModel(
                repository = repository,
                preparePhotoViewerSendUri = FakePreparePhotoViewerSendUri(
                    preparedUri = preparedUri,
                ),
            )

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onShareClick()
                assertEquals(
                    PhotoViewerEffect.Share(
                        uri = preparedUri,
                        contentType = IMAGE_JPEG,
                    ),
                    awaitItem(),
                )

                viewModel.onForwardClick()
                assertEquals(
                    PhotoViewerEffect.Forward(
                        uri = preparedUri,
                        contentType = IMAGE_JPEG,
                    ),
                    awaitItem(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun shareAndForward_whenUriPreparationFails_emitNoEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(
                repository = repository,
                preparePhotoViewerSendUri = FakePreparePhotoViewerSendUri(
                    shouldFail = true,
                ),
            )

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onShareClick()
                viewModel.onForwardClick()
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun actions_whenCurrentItemDisallowsActions_emitNoEffects() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)
            val nonActionableItem = photo1.copy(canUseActions = false)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(nonActionableItem),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            viewModel.effects.test {
                viewModel.onSaveClick()
                viewModel.onShareClick()
                viewModel.onForwardClick()
                advanceUntilIdle()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun closeClick_whenInCarousel_preservesCarouselDisplayMode() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            loadInitialItems(
                repositoryResults = repositoryResults,
                viewModel = viewModel,
            )
            viewModel.onMetadataClick()
            viewModel.onCloseClick()

            assertEquals(PhotoViewerDisplayMode.Carousel, viewModel.uiState.value.displayMode)
            assertEquals(false, viewModel.uiState.value.isMetadataSheetVisible)
            assertEquals(true, viewModel.uiState.value.isClosing)
        }
    }

    @Test
    fun closeClick_whenInImmersive_preservesImmersiveDisplayMode() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            loadInitialItems(
                repositoryResults = repositoryResults,
                viewModel = viewModel,
            )
            viewModel.onEnterImmersiveMode()
            viewModel.onCloseClick()

            assertEquals(PhotoViewerDisplayMode.Immersive, viewModel.uiState.value.displayMode)
            assertEquals(false, viewModel.uiState.value.isMetadataSheetVisible)
            assertEquals(true, viewModel.uiState.value.isClosing)
        }
    }

    @Test
    fun closeAnimationFinished_whenCalledTwice_emitsFinishOnce() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.effects.test {
                viewModel.onCloseAnimationFinished()
                viewModel.onCloseAnimationFinished()
                advanceUntilIdle()

                assertEquals(PhotoViewerEffect.Finish, awaitItem())
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun loadError_setsErrorState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitError()
            advanceUntilIdle()

            assertEquals(PhotoViewerLoadState.Error, viewModel.uiState.value.loadState)
            assertEquals(0, viewModel.uiState.value.items.size)
            assertEquals(0, viewModel.uiState.value.currentPage)
        }
    }

    @Test
    fun emptyResult_setsEmptyState() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = emptyList(),
                    initialIndex = 0,
                ),
            )
            advanceUntilIdle()

            assertEquals(PhotoViewerLoadState.Empty, viewModel.uiState.value.loadState)
            assertEquals(0, viewModel.uiState.value.items.size)
        }
    }

    @Test
    fun errorThenLoadedResult_recoversAndUsesInitialIndex() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val repositoryResults = MutableSharedFlow<PhotoViewerItemsLoadResult>(replay = 1)
            val repository = mockPhotoViewerRepository(results = repositoryResults)
            val viewModel = createViewModel(repository = repository)

            viewModel.onLaunchRequest(launchRequest = launchRequest)
            repositoryResults.emitError()
            advanceUntilIdle()

            repositoryResults.emitLoaded(
                photoViewerItems(
                    items = listOf(photo1, photo2, photo3),
                    initialIndex = 2,
                ),
            )
            advanceUntilIdle()

            assertEquals(PhotoViewerLoadState.Loaded, viewModel.uiState.value.loadState)
            assertEquals(2, viewModel.uiState.value.currentPage)
            assertEquals(photo3.contentUri, viewModel.uiState.value.items[2].contentUri)
        }
    }

    private suspend fun TestScope.loadInitialItems(
        repositoryResults: MutableSharedFlow<PhotoViewerItemsLoadResult>,
        viewModel: PhotoViewerViewModel,
    ) {
        viewModel.onLaunchRequest(launchRequest = launchRequest)
        repositoryResults.emitLoaded(
            photoViewerItems(
                items = listOf(photo1, photo2),
                initialIndex = 0,
            ),
        )
        advanceUntilIdle()
    }

    private fun createViewModel(
        repository: PhotoViewerRepository,
        normalizePhotoViewerUri: NormalizePhotoViewerUri = NormalizePhotoViewerUriImpl(),
        preparePhotoViewerSendUri: PreparePhotoViewerSendUri = FakePreparePhotoViewerSendUri(),
    ): PhotoViewerViewModel {
        return PhotoViewerViewModel(
            photoViewerRepository = repository,
            normalizePhotoViewerUri = normalizePhotoViewerUri,
            preparePhotoViewerSendUri = preparePhotoViewerSendUri,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun photoViewerItems(
        items: List<PhotoViewerItem>,
        initialIndex: Int,
    ): PhotoViewerItems {
        return PhotoViewerItems(
            items = items.toImmutableList(),
            initialIndex = initialIndex,
        )
    }

    private fun mockPhotoViewerRepository(
        results: MutableSharedFlow<PhotoViewerItemsLoadResult>,
    ): PhotoViewerRepository {
        val repository = mockk<PhotoViewerRepository>()
        every {
            repository.getPhotoViewerItems(
                photosUri = any(),
                initialPhotoUri = any(),
                initialPhotoOccurrenceIndex = any(),
            )
        } returns results

        return repository
    }

    private suspend fun MutableSharedFlow<PhotoViewerItemsLoadResult>.emitLoaded(
        items: PhotoViewerItems,
    ) {
        emit(
            PhotoViewerItemsLoadResult.Loaded(
                photoViewerItems = items,
            ),
        )
    }

    private suspend fun MutableSharedFlow<PhotoViewerItemsLoadResult>.emitError() {
        emit(PhotoViewerItemsLoadResult.Error)
    }

    private companion object {
        const val IMAGE_JPEG = "image/jpeg"

        val launchRequest = PhotoViewerLaunchRequest(
            initialPhotoUri = "content://example/content/1",
            photosUri = "content://example/photos",
            sourceBounds = PhotoViewerSourceBounds(),
        )
        val photo1 = photoViewerItem(index = 1)
        val photo2 = photoViewerItem(index = 2)
        val photo3 = photoViewerItem(index = 3)
        val photo3Refreshed = photo3.copy(
            contentUri = Uri.parse("content://example/content/3?updated=true"),
        )

        fun photoViewerItem(index: Int): PhotoViewerItem {
            return PhotoViewerItem(
                contentUri = Uri.parse("content://example/content/$index"),
                contentType = IMAGE_JPEG,
                senderName = "Ada Lovelace",
                senderDestination = "+1555123000$index",
                receivedTimestampMillis = 1_735_689_600_000L + index,
                isDraft = false,
            )
        }
    }

    private class FakePreparePhotoViewerSendUri(
        private val preparedUri: Uri? = null,
        private val shouldFail: Boolean = false,
    ) : PreparePhotoViewerSendUri {

        override fun invoke(uri: Uri): Flow<Uri> {
            return when {
                shouldFail -> emptyFlow()
                preparedUri != null -> flowOf(preparedUri)
                else -> flowOf(uri)
            }
        }
    }
}
