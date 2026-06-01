package com.android.messaging.ui.conversation.mediapicker.component.capture

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.conversation.mediapicker.ConversationCaptureMode
import com.android.messaging.ui.conversation.mediapicker.component.capture.ConversationMediaCaptureShutterPhase.Photo
import com.android.messaging.ui.conversation.mediapicker.component.capture.ConversationMediaCaptureShutterPhase.VideoIdle
import com.android.messaging.ui.conversation.mediapicker.component.capture.ConversationMediaCaptureShutterPhase.VideoRecording
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContainerColor
import com.android.messaging.ui.conversation.mediapicker.component.pickerOverlayContentColor
import com.android.messaging.ui.core.MessagingPreviewColumn

private val PICKER_SHUTTER_BORDER_WIDTH = 3.dp
private val PICKER_SHUTTER_OUTER_SIZE = 78.dp
private val PICKER_SHUTTER_PHOTO_INNER_SIZE = 62.dp
private val PICKER_SHUTTER_FULL_INNER_SIZE = PICKER_SHUTTER_OUTER_SIZE -
    (PICKER_SHUTTER_BORDER_WIDTH * 2)
private const val PICKER_SHUTTER_STATE_TRANSITION_SPRING_DAMPING_RATIO = 0.7f
private const val PICKER_SHUTTER_STATE_TRANSITION_SPRING_STIFFNESS = 500f
private val PICKER_SHUTTER_COLOR_ANIMATION_SPEC = tween<Color>(durationMillis = 180)
private val PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC = spring<Float>(
    dampingRatio = PICKER_SHUTTER_STATE_TRANSITION_SPRING_DAMPING_RATIO,
    stiffness = PICKER_SHUTTER_STATE_TRANSITION_SPRING_STIFFNESS,
)

@Composable
internal fun ConversationMediaCaptureShutterButton(
    modifier: Modifier = Modifier,
    captureMode: ConversationCaptureMode,
    isPhotoCaptureInProgress: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isEnabled = captureMode != ConversationCaptureMode.Photo || !isPhotoCaptureInProgress
    val shutterPhase = resolveConversationMediaCaptureShutterPhase(
        captureMode = captureMode,
        isRecording = isRecording,
    )
    ConversationMediaCaptureShutterButtonAnimatedContent(
        modifier = modifier,
        colorScheme = colorScheme,
        isEnabled = isEnabled,
        onClick = onClick,
        shutterPhase = shutterPhase,
    )
}

@Composable
private fun ConversationMediaCaptureShutterButtonAnimatedContent(
    modifier: Modifier,
    colorScheme: ColorScheme,
    isEnabled: Boolean,
    onClick: () -> Unit,
    shutterPhase: ConversationMediaCaptureShutterPhase,
) {
    val visualState = animateShutterVisualState(
        colorScheme = colorScheme,
        shutterPhase = shutterPhase,
    )

    ConversationMediaCaptureShutterButtonShell(
        modifier = modifier,
        borderColor = pickerOverlayContentColor(),
        isEnabled = isEnabled,
        onClick = onClick,
        outerContainerColor = visualState.outerContainerColor,
        outerScale = visualState.outerScale,
    ) {
        ConversationMediaCaptureShutterInnerDisc(
            innerShutterColor = visualState.innerShutterColor,
            innerShutterSize = visualState.innerShutterSize,
        ) {
            if (shutterPhase != Photo) {
                ConversationMediaCaptureVideoOverlay(
                    recordingStopAlpha = visualState.recordingStopAlpha,
                    recordingStopBackgroundColor = visualState.recordingStopBackgroundColor,
                    recordingStopScale = visualState.recordingStopScale,
                    videoCenterDotAlpha = visualState.videoCenterDotAlpha,
                    videoCenterDotColor = visualState.videoCenterDotColor,
                    videoCenterDotScale = visualState.videoCenterDotScale,
                )
            }
        }
    }
}

@Composable
private fun animateShutterVisualState(
    colorScheme: ColorScheme,
    shutterPhase: ConversationMediaCaptureShutterPhase,
): ConversationMediaCaptureShutterVisualState {
    val transition = updateTransition(
        targetState = shutterPhase,
        label = "picker_shutter_phase",
    )
    val surfaceVisualState = transition.animateShutterSurfaceVisualState(
        colorScheme = colorScheme,
    )
    val recordingStopVisualState =
        transition.animateRecordingStopVisualState(
            colorScheme = colorScheme,
        )
    val videoCenterDotVisualState =
        transition.animateVideoCenterDotVisualState(
            colorScheme = colorScheme,
        )
    val targetVisualState = shutterPhase.toVisualState(colorScheme = colorScheme)

    return ConversationMediaCaptureShutterVisualState(
        innerShutterColor = surfaceVisualState.innerShutterColor,
        innerShutterSize = surfaceVisualState.innerShutterSize,
        outerContainerColor = surfaceVisualState.outerContainerColor,
        outerScale = surfaceVisualState.outerScale,
        recordingStopAlpha = recordingStopVisualState.alpha,
        recordingStopBackgroundColor = targetVisualState.recordingStopBackgroundColor,
        recordingStopScale = recordingStopVisualState.scale,
        videoCenterDotAlpha = videoCenterDotVisualState.alpha,
        videoCenterDotColor = targetVisualState.videoCenterDotColor,
        videoCenterDotScale = videoCenterDotVisualState.scale,
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateShutterSurfaceVisualState(
    colorScheme: ColorScheme,
): ConversationMediaCaptureShutterSurfaceVisualState {
    val innerShutterColor by animateColor(
        transitionSpec = {
            PICKER_SHUTTER_COLOR_ANIMATION_SPEC
        },
        label = "picker_shutter_inner_color",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).innerShutterColor
        },
    )
    val innerShutterSize by animateDp(
        transitionSpec = {
            spring(
                dampingRatio = PICKER_SHUTTER_STATE_TRANSITION_SPRING_DAMPING_RATIO,
                stiffness = PICKER_SHUTTER_STATE_TRANSITION_SPRING_STIFFNESS,
            )
        },
        label = "picker_shutter_inner_size",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).innerShutterSize
        },
    )
    val outerContainerColor by animateColor(
        transitionSpec = {
            PICKER_SHUTTER_COLOR_ANIMATION_SPEC
        },
        label = "picker_shutter_outer_color",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).outerContainerColor
        },
    )
    val outerScale by animateFloat(
        transitionSpec = {
            PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC
        },
        label = "picker_shutter_outer_scale",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).outerScale
        },
    )

    return ConversationMediaCaptureShutterSurfaceVisualState(
        innerShutterColor = innerShutterColor,
        innerShutterSize = innerShutterSize,
        outerContainerColor = outerContainerColor,
        outerScale = outerScale,
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateRecordingStopVisualState(
    colorScheme: ColorScheme,
): ConversationMediaCaptureRecordingStopVisualState {
    val recordingStopAlpha by animateFloat(
        transitionSpec = {
            tween(durationMillis = 130)
        },
        label = "picker_shutter_recording_stop_alpha",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).recordingStopAlpha
        },
    )
    val recordingStopScale by animateFloat(
        transitionSpec = {
            PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC
        },
        label = "picker_shutter_recording_stop_scale",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).recordingStopScale
        },
    )

    return ConversationMediaCaptureRecordingStopVisualState(
        alpha = recordingStopAlpha,
        scale = recordingStopScale,
    )
}

@Composable
private fun Transition<ConversationMediaCaptureShutterPhase>.animateVideoCenterDotVisualState(
    colorScheme: ColorScheme,
): ConversationMediaCaptureVideoCenterDotVisualState {
    val videoCenterDotAlpha by animateFloat(
        transitionSpec = {
            tween(durationMillis = 110)
        },
        label = "picker_shutter_video_center_dot_alpha",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).videoCenterDotAlpha
        },
    )
    val videoCenterDotScale by animateFloat(
        transitionSpec = {
            PICKER_SHUTTER_FLOAT_SPRING_ANIMATION_SPEC
        },
        label = "picker_shutter_video_center_dot_scale",
        targetValueByState = { phase ->
            phase.toVisualState(colorScheme = colorScheme).videoCenterDotScale
        },
    )

    return ConversationMediaCaptureVideoCenterDotVisualState(
        alpha = videoCenterDotAlpha,
        scale = videoCenterDotScale,
    )
}

@Composable
private fun ConversationMediaCaptureShutterButtonShell(
    modifier: Modifier,
    borderColor: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    outerContainerColor: Color,
    outerScale: Float,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(PICKER_SHUTTER_OUTER_SIZE)
            .graphicsLayer {
                alpha = if (isEnabled) 1f else 0.7f
                scaleX = outerScale
                scaleY = outerScale
            },
        enabled = isEnabled,
        onClick = onClick,
        shape = CircleShape,
        color = outerContainerColor,
        border = BorderStroke(
            width = PICKER_SHUTTER_BORDER_WIDTH,
            color = borderColor,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun ConversationMediaCaptureShutterInnerDisc(
    innerShutterColor: Color,
    innerShutterSize: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.size(innerShutterSize),
        shape = CircleShape,
        color = innerShutterColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
private fun ConversationMediaCaptureVideoOverlay(
    recordingStopAlpha: Float,
    recordingStopBackgroundColor: Color,
    recordingStopScale: Float,
    videoCenterDotAlpha: Float,
    videoCenterDotColor: Color,
    videoCenterDotScale: Float,
) {
    ConversationMediaCaptureRecordingStopGlyph(
        alpha = recordingStopAlpha,
        backgroundColor = recordingStopBackgroundColor,
        scale = recordingStopScale,
    )

    ConversationMediaCaptureVideoIdleDotGlyph(
        alpha = videoCenterDotAlpha,
        color = videoCenterDotColor,
        scale = videoCenterDotScale,
    )
}

@Composable
private fun ConversationMediaCaptureRecordingStopGlyph(
    alpha: Float,
    backgroundColor: Color,
    scale: Float,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.extraSmall,
            ),
    )
}

@Composable
private fun ConversationMediaCaptureVideoIdleDotGlyph(
    alpha: Float,
    color: Color,
    scale: Float,
) {
    Surface(
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = color,
    ) {}
}

private fun resolveConversationMediaCaptureShutterPhase(
    captureMode: ConversationCaptureMode,
    isRecording: Boolean,
): ConversationMediaCaptureShutterPhase {
    return when {
        isRecording -> VideoRecording
        captureMode == ConversationCaptureMode.Video -> VideoIdle
        else -> Photo
    }
}

@Suppress("ktlint:standard:trailing-comma-on-declaration-site")
private enum class ConversationMediaCaptureShutterPhase {
    Photo,
    VideoIdle,
    VideoRecording;

    fun toVisualState(colorScheme: ColorScheme): ConversationMediaCaptureShutterVisualState {
        return when (this) {
            Photo -> ConversationMediaCaptureShutterVisualState(
                innerShutterColor = pickerOverlayContentColor(),
                innerShutterSize = PICKER_SHUTTER_PHOTO_INNER_SIZE,
                outerContainerColor = pickerOverlayContainerColor(alpha = 0.2f),
                outerScale = 1f,
                recordingStopAlpha = 0f,
                recordingStopBackgroundColor = colorScheme.error.copy(alpha = 0.3f),
                recordingStopScale = 0.8f,
                videoCenterDotAlpha = 0f,
                videoCenterDotColor = pickerOverlayContentColor(),
                videoCenterDotScale = 0.7f,
            )

            VideoIdle -> ConversationMediaCaptureShutterVisualState(
                innerShutterColor = pickerOverlayContainerColor(alpha = 0.5f),
                innerShutterSize = PICKER_SHUTTER_FULL_INNER_SIZE,
                outerContainerColor = Color.Transparent,
                outerScale = 1f,
                recordingStopAlpha = 0f,
                recordingStopBackgroundColor = colorScheme.error.copy(alpha = 0.3f),
                recordingStopScale = 0.8f,
                videoCenterDotAlpha = 1f,
                videoCenterDotColor = pickerOverlayContentColor(),
                videoCenterDotScale = 1f,
            )

            VideoRecording -> ConversationMediaCaptureShutterVisualState(
                innerShutterColor = colorScheme.errorContainer,
                innerShutterSize = PICKER_SHUTTER_FULL_INNER_SIZE,
                outerContainerColor = Color.Transparent,
                outerScale = 0.97f,
                recordingStopAlpha = 1f,
                recordingStopBackgroundColor = colorScheme.error.copy(alpha = 0.3f),
                recordingStopScale = 1f,
                videoCenterDotAlpha = 0f,
                videoCenterDotColor = pickerOverlayContentColor(),
                videoCenterDotScale = 0.7f,
            )
        }
    }
}

private data class ConversationMediaCaptureShutterVisualState(
    val innerShutterColor: Color,
    val innerShutterSize: Dp,
    val outerContainerColor: Color,
    val outerScale: Float,
    val recordingStopAlpha: Float,
    val recordingStopBackgroundColor: Color,
    val recordingStopScale: Float,
    val videoCenterDotAlpha: Float,
    val videoCenterDotColor: Color,
    val videoCenterDotScale: Float,
)

private data class ConversationMediaCaptureShutterSurfaceVisualState(
    val innerShutterColor: Color,
    val innerShutterSize: Dp,
    val outerContainerColor: Color,
    val outerScale: Float,
)

private data class ConversationMediaCaptureRecordingStopVisualState(
    val alpha: Float,
    val scale: Float,
)

private data class ConversationMediaCaptureVideoCenterDotVisualState(
    val alpha: Float,
    val scale: Float,
)

@PreviewLightDark
@Composable
private fun ConversationMediaCaptureShutterButtonPreview() {
    MessagingPreviewColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            ConversationMediaCaptureShutterButton(
                captureMode = ConversationCaptureMode.Photo,
                isPhotoCaptureInProgress = false,
                isRecording = false,
                onClick = {},
            )
            ConversationMediaCaptureShutterButton(
                captureMode = ConversationCaptureMode.Photo,
                isPhotoCaptureInProgress = true,
                isRecording = false,
                onClick = {},
            )
            ConversationMediaCaptureShutterButton(
                captureMode = ConversationCaptureMode.Video,
                isPhotoCaptureInProgress = false,
                isRecording = false,
                onClick = {},
            )
            ConversationMediaCaptureShutterButton(
                captureMode = ConversationCaptureMode.Video,
                isPhotoCaptureInProgress = false,
                isRecording = true,
                onClick = {},
            )
        }
    }
}
