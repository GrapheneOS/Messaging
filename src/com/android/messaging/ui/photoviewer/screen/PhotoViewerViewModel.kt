package com.android.messaging.ui.photoviewer.screen

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.messaging.data.media.model.PhotoViewerItem
import com.android.messaging.data.media.model.PhotoViewerItems
import com.android.messaging.data.media.model.PhotoViewerItemsLoadResult
import com.android.messaging.data.media.repository.PhotoViewerRepository
import com.android.messaging.di.core.DefaultDispatcher
import com.android.messaging.domain.photoviewer.usecase.NormalizePhotoViewerUri
import com.android.messaging.domain.photoviewer.usecase.PreparePhotoViewerSendUri
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequestKey
import com.android.messaging.ui.photoviewer.model.photoViewerLaunchRequestKey
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerDisplayMode
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerEffect
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerLoadState
import com.android.messaging.ui.photoviewer.screen.model.PhotoViewerUiState
import com.android.messaging.util.LogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface PhotoViewerScreenModel {
    val effects: Flow<PhotoViewerEffect>
    val uiState: StateFlow<PhotoViewerUiState>

    fun onLaunchRequest(launchRequest: PhotoViewerLaunchRequest)
    fun onPageSettled(page: Int)
    fun onToggleDisplayMode()
    fun onEnterImmersiveMode()
    fun onMetadataClick()
    fun onMetadataDismissed()
    fun onCloseClick()
    fun onCloseAnimationFinished()
    fun onSaveClick()
    fun onShareClick()
    fun onForwardClick()
}

@HiltViewModel
internal class PhotoViewerViewModel @Inject constructor(
    private val photoViewerRepository: PhotoViewerRepository,
    private val normalizePhotoViewerUri: NormalizePhotoViewerUri,
    private val preparePhotoViewerSendUri: PreparePhotoViewerSendUri,
    @param:DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel(),
    PhotoViewerScreenModel {

    private val _uiState = MutableStateFlow(PhotoViewerUiState())
    private val effectsChannel = Channel<PhotoViewerEffect>(
        capacity = Channel.BUFFERED,
    )
    private var launchRequestKey: PhotoViewerLaunchRequestKey? = null
    private var loadJob: Job? = null
    private var hasFinished = false

    override val effects = effectsChannel.receiveAsFlow()
    override val uiState = _uiState.asStateFlow()

    override fun onLaunchRequest(launchRequest: PhotoViewerLaunchRequest) {
        val nextLaunchRequestKey = photoViewerLaunchRequestKey(launchRequest = launchRequest)
        if (launchRequestKey == nextLaunchRequestKey) {
            return
        }

        launchRequestKey = nextLaunchRequestKey
        hasFinished = false
        loadJob?.cancel()
        _uiState.value = PhotoViewerUiState(loadState = PhotoViewerLoadState.Loading)

        loadJob = viewModelScope.launch(defaultDispatcher) {
            var isInitialEmission = true

            photoViewerRepository
                .getPhotoViewerItems(
                    photosUri = launchRequest.photosUri.toUri(),
                    initialPhotoUri = launchRequest.initialPhotoUri.toUri(),
                    initialPhotoOccurrenceIndex = launchRequest.initialPhotoOccurrenceIndex,
                )
                .catch { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }

                    LogUtil.e(TAG, "Photo viewer item flow failed", throwable)
                    setPhotoViewerItemsLoadError()
                }
                .collect { loadResult ->
                    val didApplySuccessfulResult = applyPhotoViewerItemsLoadResult(
                        loadResult = loadResult,
                        useInitialIndex = isInitialEmission,
                    )

                    if (didApplySuccessfulResult) {
                        isInitialEmission = false
                    }
                }
        }
    }

    override fun onPageSettled(page: Int) {
        setCurrentPage(page = page)
    }

    override fun onToggleDisplayMode() {
        _uiState.update { state ->
            when {
                state.isClosing -> state
                state.displayMode == PhotoViewerDisplayMode.Carousel -> {
                    state.copy(displayMode = PhotoViewerDisplayMode.Immersive)
                }

                else -> {
                    state.copy(displayMode = PhotoViewerDisplayMode.Carousel)
                }
            }
        }
    }

    override fun onEnterImmersiveMode() {
        _uiState.update { state ->
            when {
                state.isClosing -> state
                state.displayMode == PhotoViewerDisplayMode.Immersive -> state
                else -> {
                    state.copy(displayMode = PhotoViewerDisplayMode.Immersive)
                }
            }
        }
    }

    override fun onMetadataClick() {
        _uiState.update { state ->
            when {
                state.isClosing -> state
                state.items.isEmpty() -> state
                else -> {
                    state.copy(
                        displayMode = PhotoViewerDisplayMode.Carousel,
                        isMetadataSheetVisible = true,
                    )
                }
            }
        }
    }

    override fun onMetadataDismissed() {
        _uiState.update { it.copy(isMetadataSheetVisible = false) }
    }

    override fun onCloseClick() {
        _uiState.update { state ->
            when {
                state.isClosing -> state
                else -> {
                    state.copy(
                        isMetadataSheetVisible = false,
                        isClosing = true,
                    )
                }
            }
        }
    }

    override fun onCloseAnimationFinished() {
        if (hasFinished) {
            return
        }

        hasFinished = true
        sendEffect(effect = PhotoViewerEffect.Finish)
    }

    override fun onSaveClick() {
        currentItemForAction()?.let { item ->
            sendEffect(
                effect = PhotoViewerEffect.Save(
                    uri = item.contentUri,
                    contentType = item.contentType,
                ),
            )
        }
    }

    override fun onShareClick() {
        sendPreparedPhotoEffect { item, preparedUri ->
            PhotoViewerEffect.Share(
                uri = preparedUri,
                contentType = item.contentType,
            )
        }
    }

    override fun onForwardClick() {
        sendPreparedPhotoEffect { item, preparedUri ->
            PhotoViewerEffect.Forward(
                uri = preparedUri,
                contentType = item.contentType,
            )
        }
    }

    private fun sendEffect(effect: PhotoViewerEffect) {
        viewModelScope.launch(defaultDispatcher) {
            effectsChannel.send(element = effect)
        }
    }

    private fun sendPreparedPhotoEffect(
        buildEffect: (PhotoViewerItem, Uri) -> PhotoViewerEffect,
    ) {
        val item = currentItemForAction() ?: return

        viewModelScope.launch(defaultDispatcher) {
            preparePhotoViewerSendUri(uri = item.contentUri)
                .map { preparedUri ->
                    buildEffect(item, preparedUri)
                }
                .collect { element ->
                    effectsChannel.send(element = element)
                }
        }
    }

    private fun setCurrentPage(page: Int) {
        _uiState.update { currentState ->
            val coercedPage = page.coerceIn(
                minimumValue = 0,
                maximumValue = currentState.items.lastIndex.coerceAtLeast(minimumValue = 0),
            )

            currentState.copy(currentPage = coercedPage)
        }
    }

    private fun currentItemForAction(): PhotoViewerItem? {
        val state = uiState.value

        return state
            .items
            .getOrNull(index = state.currentPage)
            ?.takeIf { item -> item.canUseActions }
    }

    private fun updatedStateForPhotoViewerItems(
        currentState: PhotoViewerUiState,
        result: PhotoViewerItems,
        useInitialIndex: Boolean,
    ): PhotoViewerUiState {
        return currentState.copy(
            loadState = loadStateForItems(items = result.items),
            items = result.items,
            currentPage = resolveCurrentPage(
                currentState = currentState,
                items = result.items,
                initialIndex = result.initialIndex,
                useInitialIndex = useInitialIndex,
            ),
            displayMode = when {
                useInitialIndex -> PhotoViewerDisplayMode.Carousel
                else -> currentState.displayMode
            },
            isMetadataSheetVisible = when {
                useInitialIndex -> false
                result.items.isEmpty() -> false
                else -> currentState.isMetadataSheetVisible
            },
            isClosing = when {
                useInitialIndex -> false
                else -> currentState.isClosing
            },
        )
    }

    private fun applyPhotoViewerItemsLoadResult(
        loadResult: PhotoViewerItemsLoadResult,
        useInitialIndex: Boolean,
    ): Boolean {
        var didApplySuccessfulResult = false

        when (loadResult) {
            is PhotoViewerItemsLoadResult.Loaded -> {
                _uiState.update { currentState ->
                    updatedStateForPhotoViewerItems(
                        currentState = currentState,
                        result = loadResult.photoViewerItems,
                        useInitialIndex = useInitialIndex,
                    )
                }
                didApplySuccessfulResult = true
            }

            PhotoViewerItemsLoadResult.Error -> {
                setPhotoViewerItemsLoadError()
            }
        }

        return didApplySuccessfulResult
    }

    private fun loadStateForItems(items: ImmutableList<PhotoViewerItem>): PhotoViewerLoadState {
        return when {
            items.isEmpty() -> PhotoViewerLoadState.Empty
            else -> PhotoViewerLoadState.Loaded
        }
    }

    private fun setPhotoViewerItemsLoadError() {
        _uiState.update { currentState ->
            currentState.copy(
                loadState = PhotoViewerLoadState.Error,
                items = persistentListOf(),
                currentPage = 0,
                displayMode = PhotoViewerDisplayMode.Carousel,
                isMetadataSheetVisible = false,
            )
        }
    }

    private fun resolveCurrentPage(
        currentState: PhotoViewerUiState,
        items: ImmutableList<PhotoViewerItem>,
        initialIndex: Int,
        useInitialIndex: Boolean,
    ): Int {
        if (items.isEmpty()) {
            return 0
        }

        val resolvedPage = when {
            useInitialIndex -> initialIndex
            else -> {
                currentState
                    .items
                    .getOrNull(index = currentState.currentPage)
                    ?.let { currentItem ->
                        findMatchingItemIndex(
                            items = items,
                            currentItem = currentItem,
                        )
                    }
                    ?.takeIf { it >= 0 }
                    ?: currentState.currentPage
            }
        }

        return resolvedPage.coerceIn(
            minimumValue = 0,
            maximumValue = items.lastIndex,
        )
    }

    private fun findMatchingItemIndex(
        items: ImmutableList<PhotoViewerItem>,
        currentItem: PhotoViewerItem,
    ): Int {
        val currentItemUri = normalizePhotoViewerUri(uri = currentItem.contentUri)

        return items.indexOfFirst { candidate ->
            normalizePhotoViewerUri(uri = candidate.contentUri) == currentItemUri
        }
    }

    private companion object {
        const val TAG = "PhotoViewerViewModel"
    }
}
