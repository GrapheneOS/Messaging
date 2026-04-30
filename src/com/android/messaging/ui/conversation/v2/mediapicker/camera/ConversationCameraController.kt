package com.android.messaging.ui.conversation.v2.mediapicker.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.ui.conversation.v2.mediapicker.model.ConversationCapturedMedia
import com.android.messaging.util.ContentType
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal interface ConversationCameraController {
    val hasFlashUnit: StateFlow<Boolean>
    val isPhotoCaptureInProgress: StateFlow<Boolean>
    val isRecording: StateFlow<Boolean>
    val photoFlashMode: StateFlow<ConversationPhotoFlashMode>
    val recordingDurationMillis: StateFlow<Long>
    val surfaceRequest: StateFlow<SurfaceRequest?>

    fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        onError: (Throwable) -> Unit,
    )

    fun capturePhoto(
        onCaptured: (ConversationCapturedMedia) -> Unit,
        onError: (Throwable) -> Unit,
    )

    fun startVideoRecording(
        withAudio: Boolean,
        onCaptured: (ConversationCapturedMedia) -> Unit,
        onDiscarded: () -> Unit,
        onError: (Throwable) -> Unit,
    )

    fun stopVideoRecording()
    fun cancelVideoRecording()

    fun switchCamera(onError: (Throwable) -> Unit)

    fun cyclePhotoFlashMode(onError: (Throwable) -> Unit)

    fun unbind()
}

private class ConversationCameraControllerImpl(
    context: Context,
) : ConversationCameraController {
    private val applicationContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(applicationContext)

    private val _hasFlashUnit = MutableStateFlow(false)
    private val _isPhotoCaptureInProgress = MutableStateFlow(false)
    private val _isRecording = MutableStateFlow(false)
    private val _photoFlashMode = MutableStateFlow(ConversationPhotoFlashMode.Off)
    private val _recordingDurationMillis = MutableStateFlow(0L)
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)

    override val hasFlashUnit = _hasFlashUnit.asStateFlow()
    override val isPhotoCaptureInProgress = _isPhotoCaptureInProgress.asStateFlow()
    override val isRecording = _isRecording.asStateFlow()
    override val photoFlashMode = _photoFlashMode.asStateFlow()
    override val recordingDurationMillis = _recordingDurationMillis.asStateFlow()
    override val surfaceRequest = _surfaceRequest.asStateFlow()

    private var activeRecordingSession: ActiveRecordingSession? = null
    private var bindGeneration = 0L
    private var boundCameraSession: BoundCameraSession? = null
    private var bindRequestLifecycleOwner: LifecycleOwner? = null
    private var preferredLensFacing = CameraSelector.LENS_FACING_BACK
    private var preferredPhotoFlashMode = ConversationPhotoFlashMode.Off

    override fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        onError: (Throwable) -> Unit,
    ) {
        bindRequestLifecycleOwner = lifecycleOwner
        val requestedBindGeneration = ++bindGeneration

        requestCameraProvider(
            lifecycleOwner = lifecycleOwner,
            requestedBindGeneration = requestedBindGeneration,
            onError = onError,
        )
    }

    override fun capturePhoto(
        onCaptured: (ConversationCapturedMedia) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val currentImageCapture = getReadyImageCaptureOrReportError(onError = onError) ?: return
        val photoOutput = createPhotoOutputOrReportError(onError = onError) ?: return

        capturePhotoWithOutput(
            imageCapture = currentImageCapture,
            photoOutput = photoOutput,
            onCaptured = onCaptured,
            onError = onError,
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startVideoRecording(
        withAudio: Boolean,
        onCaptured: (ConversationCapturedMedia) -> Unit,
        onDiscarded: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val currentVideoCapture = getReadyVideoCaptureOrReportError(onError = onError) ?: return
        val videoOutput = createVideoOutputOrReportError(onError = onError) ?: return
        val preparedRecording = prepareVideoRecording(
            videoCapture = currentVideoCapture,
            videoOutput = videoOutput,
            withAudio = withAudio,
        )
        val callbacks = VideoRecordingCallbacks(
            onCaptured = onCaptured,
            onDiscarded = onDiscarded,
            onError = onError,
        )

        startPreparedRecording(
            preparedRecording = preparedRecording,
            videoOutput = videoOutput,
            callbacks = callbacks,
        )
    }

    override fun stopVideoRecording() {
        val recording = updateRecordingDiscardOnFinalize(discardOnFinalize = false) ?: return
        recording.stop()
    }

    override fun cancelVideoRecording() {
        val recording = updateRecordingDiscardOnFinalize(discardOnFinalize = true) ?: return
        recording.stop()
    }

    override fun switchCamera(onError: (Throwable) -> Unit) {
        val currentBoundCameraSession =
            getBoundCameraSessionOrReportError(onError = onError) ?: return
        val targetLensFacing = resolveSwitchTargetLensFacing(
            currentLensFacing = currentBoundCameraSession.lensFacing,
        )

        runCatching {
            requireAvailableLensFacing(
                processCameraProvider = currentBoundCameraSession.cameraProvider,
                lensFacing = targetLensFacing,
            )
            rebindForLensFacing(
                boundCameraSession = currentBoundCameraSession,
                lensFacing = targetLensFacing,
            )
        }.onFailure(onError)
    }

    override fun cyclePhotoFlashMode(onError: (Throwable) -> Unit) {
        val currentBoundCameraSession =
            getBoundCameraSessionOrReportError(onError = onError) ?: return
        if (!_hasFlashUnit.value) {
            onError(FlashUnavailableException())
            return
        }

        val nextPhotoFlashMode = _photoFlashMode.value.next()
        runCatching {
            updatePhotoFlashMode(
                imageCapture = currentBoundCameraSession.imageCapture,
                photoFlashMode = nextPhotoFlashMode,
            )
        }.onFailure(onError)
    }

    private fun updatePhotoFlashMode(
        imageCapture: ImageCapture,
        photoFlashMode: ConversationPhotoFlashMode,
    ) {
        imageCapture.flashMode = photoFlashMode.imageCaptureFlashMode
        preferredPhotoFlashMode = photoFlashMode
        _photoFlashMode.value = photoFlashMode
    }

    private fun resetPhotoFlashAvailabilityState() {
        _hasFlashUnit.value = false
    }

    private fun syncBoundImageCaptureFlashMode(
        imageCapture: ImageCapture,
    ) {
        updatePhotoFlashMode(
            imageCapture = imageCapture,
            photoFlashMode = preferredPhotoFlashMode,
        )
    }

    override fun unbind() {
        invalidateCurrentBinding()
        stopRecordingForUnbind()
        clearBoundCameraReferences()
        resetUiState()
    }

    private fun requestCameraProvider(
        lifecycleOwner: LifecycleOwner,
        requestedBindGeneration: Long,
        onError: (Throwable) -> Unit,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener(
            {
                handleCameraProviderReady(
                    cameraProviderFuture = cameraProviderFuture,
                    lifecycleOwner = lifecycleOwner,
                    requestedBindGeneration = requestedBindGeneration,
                    onError = onError,
                )
            },
            mainExecutor,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleCameraProviderReady(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        lifecycleOwner: LifecycleOwner,
        requestedBindGeneration: Long,
        onError: (Throwable) -> Unit,
    ) {
        try {
            if (!isCurrentBindGeneration(bindGeneration = requestedBindGeneration)) {
                return
            }

            val processCameraProvider = cameraProviderFuture.get()
            if (!isCurrentBindGeneration(bindGeneration = requestedBindGeneration)) {
                return
            }

            rebindUseCases(
                lifecycleOwner = lifecycleOwner,
                processCameraProvider = processCameraProvider,
            )
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    private fun rebindUseCases(
        lifecycleOwner: LifecycleOwner,
        processCameraProvider: ProcessCameraProvider,
    ) {
        processCameraProvider.unbindAll()
        _surfaceRequest.value = null

        val selectedLensFacing = resolveBindLensFacing(
            processCameraProvider = processCameraProvider,
        )
        val selectedCameraSelector = buildCameraSelector(lensFacing = selectedLensFacing)
        val boundUseCases = createBoundUseCases()
        val camera = processCameraProvider.bindToLifecycle(
            lifecycleOwner,
            selectedCameraSelector,
            boundUseCases.preview,
            boundUseCases.imageCapture,
            boundUseCases.videoCapture,
        )

        preferredLensFacing = selectedLensFacing
        val newBoundCameraSession = BoundCameraSession(
            boundCamera = camera,
            cameraProvider = processCameraProvider,
            imageCapture = boundUseCases.imageCapture,
            lifecycleOwner = lifecycleOwner,
            lensFacing = selectedLensFacing,
            videoCapture = boundUseCases.videoCapture,
        )
        boundCameraSession = newBoundCameraSession
        publishBoundCameraState(boundCameraSession = newBoundCameraSession)
    }

    private fun createBoundUseCases(): BoundUseCases {
        return BoundUseCases(
            imageCapture = createImageCaptureUseCase(),
            preview = createPreviewUseCase(),
            videoCapture = createVideoCaptureUseCase(),
        )
    }

    private fun createPreviewUseCase(): Preview {
        return Preview.Builder().build().also { previewUseCase ->
            previewUseCase.setSurfaceProvider { surfaceRequest ->
                _surfaceRequest.value = surfaceRequest
            }
        }
    }

    private fun createImageCaptureUseCase(): ImageCapture {
        return ImageCapture.Builder()
            .setFlashMode(preferredPhotoFlashMode.imageCaptureFlashMode)
            .build()
    }

    private fun createVideoCaptureUseCase(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder().build()

        return VideoCapture.withOutput(recorder)
    }

    private fun publishBoundCameraState(boundCameraSession: BoundCameraSession) {
        _hasFlashUnit.value = boundCameraSession.boundCamera.cameraInfo.hasFlashUnit()
        syncBoundImageCaptureFlashMode(
            imageCapture = boundCameraSession.imageCapture,
        )
    }

    private fun getReadyImageCaptureOrReportError(
        onError: (Throwable) -> Unit,
    ): ImageCapture? {
        if (_isPhotoCaptureInProgress.value) {
            onError(PhotoCaptureAlreadyInProgressException())
            return null
        }

        val currentBoundCameraSession = getBoundCameraSessionOrReportError(onError = onError)
            ?: return null

        return currentBoundCameraSession.imageCapture
    }

    private fun createPhotoOutputOrReportError(
        onError: (Throwable) -> Unit,
    ): ScratchOutput? {
        val photoOutput = createScratchOutputOrNull(contentType = ContentType.IMAGE_JPEG)
        if (photoOutput == null) {
            onError(
                ScratchFileCreationFailedException(
                    mediaLabel = "photo",
                ),
            )
            return null
        }

        return photoOutput
    }

    private fun capturePhotoWithOutput(
        imageCapture: ImageCapture,
        photoOutput: ScratchOutput,
        onCaptured: (ConversationCapturedMedia) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoOutput.file).build()
        _isPhotoCaptureInProgress.value = true

        runCatching {
            imageCapture.takePicture(
                outputOptions,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        handlePhotoCaptureFailure(
                            photoOutput = photoOutput,
                            exception = exception,
                            onError = onError,
                        )
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        handlePhotoCaptured(
                            photoOutput = photoOutput,
                            onCaptured = onCaptured,
                        )
                    }
                },
            )
        }.onFailure { throwable ->
            _isPhotoCaptureInProgress.value = false
            deleteScratchOutput(scratchOutput = photoOutput)
            onError(
                PhotoCaptureStartFailedException(
                    cause = throwable,
                ),
            )
        }
    }

    private fun handlePhotoCaptureFailure(
        photoOutput: ScratchOutput,
        exception: ImageCaptureException,
        onError: (Throwable) -> Unit,
    ) {
        _isPhotoCaptureInProgress.value = false
        deleteScratchOutput(scratchOutput = photoOutput)
        onError(
            PhotoCaptureFailedException(
                cause = exception,
            ),
        )
    }

    private fun handlePhotoCaptured(
        photoOutput: ScratchOutput,
        onCaptured: (ConversationCapturedMedia) -> Unit,
    ) {
        _isPhotoCaptureInProgress.value = false
        onCaptured(
            ConversationCapturedMedia(
                contentUri = photoOutput.uri.toString(),
                contentType = ContentType.IMAGE_JPEG,
            ),
        )
    }

    private fun getReadyVideoCaptureOrReportError(
        onError: (Throwable) -> Unit,
    ): VideoCapture<Recorder>? {
        if (activeRecordingSession != null) {
            onError(RecordingAlreadyInProgressException())
            return null
        }

        return getBoundCameraSessionOrReportError(onError = onError)?.videoCapture
    }

    private fun createVideoOutputOrReportError(
        onError: (Throwable) -> Unit,
    ): ScratchOutput? {
        val videoOutput = createScratchOutputOrNull(contentType = ContentType.VIDEO_MP4)
        if (videoOutput == null) {
            onError(
                ScratchFileCreationFailedException(
                    mediaLabel = "video",
                ),
            )
            return null
        }

        return videoOutput
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun prepareVideoRecording(
        videoCapture: VideoCapture<Recorder>,
        videoOutput: ScratchOutput,
        withAudio: Boolean,
    ): PendingRecording {
        val outputOptions = FileOutputOptions.Builder(videoOutput.file).build()
        var preparedRecording = videoCapture.output.prepareRecording(
            applicationContext,
            outputOptions,
        )

        if (withAudio) {
            preparedRecording = preparedRecording.withAudioEnabled()
        }

        return preparedRecording
    }

    private fun startPreparedRecording(
        preparedRecording: PendingRecording,
        videoOutput: ScratchOutput,
        callbacks: VideoRecordingCallbacks,
    ) {
        runCatching {
            preparedRecording.start(mainExecutor) { event ->
                handleVideoRecordEvent(
                    event = event,
                    callbacks = callbacks,
                )
            }
        }.onSuccess { recording ->
            activeRecordingSession = ActiveRecordingSession(
                discardOnFinalize = false,
                recording = recording,
                scratchOutput = videoOutput,
            )
        }.onFailure { throwable ->
            deleteScratchOutput(scratchOutput = videoOutput)
            callbacks.onError(throwable)
        }
    }

    private fun handleVideoRecordEvent(
        event: VideoRecordEvent,
        callbacks: VideoRecordingCallbacks,
    ) {
        when (event) {
            is VideoRecordEvent.Finalize -> {
                handleVideoRecordingFinalized(
                    event = event,
                    callbacks = callbacks,
                )
            }

            is VideoRecordEvent.Start -> handleVideoRecordingStarted()

            is VideoRecordEvent.Status -> handleVideoRecordingStatus(event = event)
        }
    }

    private fun handleVideoRecordingStarted() {
        _isRecording.value = true
        _recordingDurationMillis.value = 0L
    }

    private fun handleVideoRecordingStatus(event: VideoRecordEvent.Status) {
        _recordingDurationMillis.value =
            event.recordingStats.recordedDurationNanos / NANOS_PER_MILLISECOND
    }

    private fun handleVideoRecordingFinalized(
        event: VideoRecordEvent.Finalize,
        callbacks: VideoRecordingCallbacks,
    ) {
        val recordingSession = clearRecordingSession()
        val recordingOutput = recordingSession?.scratchOutput

        when {
            recordingSession?.discardOnFinalize == true -> {
                deleteScratchOutput(scratchOutput = recordingOutput)
                callbacks.onDiscarded()
            }

            event.error == VideoRecordEvent.Finalize.ERROR_NONE -> {
                callbacks.onCaptured(
                    ConversationCapturedMedia(
                        contentUri = requireNotNull(recordingOutput).uri.toString(),
                        contentType = ContentType.VIDEO_MP4,
                    ),
                )
            }

            else -> {
                deleteScratchOutput(scratchOutput = recordingOutput)
                callbacks.onError(createVideoRecordingFailedException(event = event))
            }
        }
    }

    private fun clearRecordingSession(): ActiveRecordingSession? {
        _isRecording.value = false
        _recordingDurationMillis.value = 0L
        val recordingSession = activeRecordingSession
        activeRecordingSession = null

        return recordingSession
    }

    private fun invalidateCurrentBinding() {
        bindGeneration += 1
    }

    private fun stopRecordingForUnbind() {
        val recording = updateRecordingDiscardOnFinalize(discardOnFinalize = true)
        recording?.stop()
    }

    private fun clearBoundCameraReferences() {
        boundCameraSession?.cameraProvider?.unbindAll()
        boundCameraSession = null

        bindRequestLifecycleOwner = null
    }

    private fun resetUiState() {
        resetPhotoFlashAvailabilityState()

        _isPhotoCaptureInProgress.value = false
        _isRecording.value = false
        _recordingDurationMillis.value = 0L
        _surfaceRequest.value = null
    }

    private fun resolveSwitchTargetLensFacing(currentLensFacing: Int): Int {
        return when (currentLensFacing) {
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_BACK
            else -> CameraSelector.LENS_FACING_FRONT
        }
    }

    private fun requireAvailableLensFacing(
        processCameraProvider: ProcessCameraProvider,
        lensFacing: Int,
    ) {
        val cameraSelector = buildCameraSelector(lensFacing = lensFacing)
        if (!processCameraProvider.hasCamera(cameraSelector)) {
            throw CameraLensUnavailableException(
                lensFacing = lensFacing,
            )
        }
    }

    private fun rebindForLensFacing(
        boundCameraSession: BoundCameraSession,
        lensFacing: Int,
    ) {
        preferredLensFacing = lensFacing
        rebindUseCases(
            lifecycleOwner = boundCameraSession.lifecycleOwner,
            processCameraProvider = boundCameraSession.cameraProvider,
        )
    }

    private fun resolveBindLensFacing(
        processCameraProvider: ProcessCameraProvider,
    ): Int {
        val preferredCameraSelector = buildCameraSelector(lensFacing = preferredLensFacing)
        if (processCameraProvider.hasCamera(preferredCameraSelector)) {
            return preferredLensFacing
        }

        val fallbackLensFacing = when (preferredLensFacing) {
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_BACK
            else -> CameraSelector.LENS_FACING_FRONT
        }

        val fallbackCameraSelector = buildCameraSelector(lensFacing = fallbackLensFacing)
        if (!processCameraProvider.hasCamera(fallbackCameraSelector)) {
            throw CameraLensUnavailableException(
                lensFacing = fallbackLensFacing,
            )
        }

        return fallbackLensFacing
    }

    private fun buildCameraSelector(lensFacing: Int): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    private fun createScratchOutputOrNull(contentType: String): ScratchOutput? {
        val scratchFileUri = MediaScratchFileProvider.buildMediaScratchSpaceUri(
            resolveScratchFileExtension(contentType = contentType),
        )

        return MediaScratchFileProvider.getFileFromUri(scratchFileUri)?.let { scratchFile ->
            ScratchOutput(
                file = scratchFile,
                uri = scratchFileUri,
            )
        }
    }

    private fun deleteScratchOutput(scratchOutput: ScratchOutput?) {
        if (scratchOutput == null) {
            return
        }

        applicationContext.contentResolver.delete(scratchOutput.uri, null, null)
    }

    private fun isCurrentBindGeneration(bindGeneration: Long): Boolean {
        return this.bindGeneration == bindGeneration && bindRequestLifecycleOwner != null
    }

    private fun getBoundCameraSessionOrReportError(
        onError: (Throwable) -> Unit,
    ): BoundCameraSession? {
        val currentBoundCameraSession = boundCameraSession
        if (currentBoundCameraSession == null) {
            onError(CameraNotBoundException())
            return null
        }

        return currentBoundCameraSession
    }

    private fun updateRecordingDiscardOnFinalize(discardOnFinalize: Boolean): Recording? {
        val currentRecordingSession = activeRecordingSession ?: return null
        activeRecordingSession = currentRecordingSession.copy(
            discardOnFinalize = discardOnFinalize,
        )

        return currentRecordingSession.recording
    }

    private fun createVideoRecordingFailedException(
        event: VideoRecordEvent.Finalize,
    ): VideoRecordingFailedException {
        return VideoRecordingFailedException(
            cause = event.cause,
            errorCode = event.error,
            errorName = resolveVideoRecordingErrorName(errorCode = event.error),
        )
    }

    private fun resolveVideoRecordingErrorName(errorCode: Int): String {
        return when (errorCode) {
            VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED -> "duration_limit_reached"
            VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED -> "encoding_failed"
            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED -> "file_size_limit_reached"
            VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE -> "insufficient_storage"
            VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS -> "invalid_output_options"
            VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> "no_valid_data"
            VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR -> "recorder_error"
            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> "source_inactive"
            else -> "unknown"
        }
    }

    private data class BoundUseCases(
        val imageCapture: ImageCapture,
        val preview: Preview,
        val videoCapture: VideoCapture<Recorder>,
    )

    private data class BoundCameraSession(
        val boundCamera: Camera,
        val cameraProvider: ProcessCameraProvider,
        val imageCapture: ImageCapture,
        val lifecycleOwner: LifecycleOwner,
        val lensFacing: Int,
        val videoCapture: VideoCapture<Recorder>,
    )

    private data class ActiveRecordingSession(
        val discardOnFinalize: Boolean,
        val recording: Recording,
        val scratchOutput: ScratchOutput,
    )

    private data class ScratchOutput(
        val file: File,
        val uri: Uri,
    )

    private data class VideoRecordingCallbacks(
        val onCaptured: (ConversationCapturedMedia) -> Unit,
        val onDiscarded: () -> Unit,
        val onError: (Throwable) -> Unit,
    )

    private fun resolveScratchFileExtension(contentType: String): String {
        val mimeTypeExtension = ContentType.getExtensionFromMimeType(contentType)

        return mimeTypeExtension ?: ContentType.getExtension(contentType)
    }

    private companion object {
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

@Composable
internal fun rememberConversationCameraController(): ConversationCameraController {
    val context = LocalContext.current

    return remember(context) {
        ConversationCameraControllerImpl(
            context = context,
        )
    }
}
