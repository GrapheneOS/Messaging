package com.android.messaging.ui.conversation.mediapicker.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.LifecycleOwner
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BindConversationCameraLifecycleEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val cameraController = mockk<ConversationCameraController>(relaxed = true)
    private val lifecycleOwner = mockk<LifecycleOwner>()

    @Before
    fun setUpBindConversationCameraLifecycleEffectTest() {
        clearAllMocks()
    }

    @Test
    fun permissionGrantedAndPreviewVisible_bindsCameraAndUnbindsWhenOwnerChanges() {
        val firstLifecycleOwner = lifecycleOwner
        val secondLifecycleOwner = mockk<LifecycleOwner>()
        var currentLifecycleOwner by mutableStateOf(firstLifecycleOwner)

        composeTestRule.setContent {
            BindConversationCameraLifecycleEffect(
                cameraController = cameraController,
                cameraPermissionGranted = true,
                isCameraPreviewVisible = true,
                lifecycleOwner = currentLifecycleOwner,
            )
        }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.bindToLifecycle(
                    lifecycleOwner = firstLifecycleOwner,
                    onError = any(),
                )
            }
            verify(exactly = 0) {
                cameraController.unbind()
            }
        }

        composeTestRule.runOnIdle {
            currentLifecycleOwner = secondLifecycleOwner
        }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.unbind()
            }
            verify(exactly = 1) {
                cameraController.bindToLifecycle(
                    lifecycleOwner = secondLifecycleOwner,
                    onError = any(),
                )
            }
        }
    }

    @Test
    fun permissionDenied_unbindsWithoutBinding() {
        setContent(
            cameraPermissionGranted = false,
            isCameraPreviewVisible = true,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.unbind()
            }
            verify(exactly = 0) {
                cameraController.bindToLifecycle(
                    lifecycleOwner = any(),
                    onError = any(),
                )
            }
        }
    }

    @Test
    fun previewHidden_unbindsWithoutBinding() {
        setContent(
            cameraPermissionGranted = true,
            isCameraPreviewVisible = false,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                cameraController.unbind()
            }
            verify(exactly = 0) {
                cameraController.bindToLifecycle(
                    lifecycleOwner = any(),
                    onError = any(),
                )
            }
        }
    }

    private fun setContent(
        cameraPermissionGranted: Boolean,
        isCameraPreviewVisible: Boolean,
    ) {
        composeTestRule.setContent {
            BindConversationCameraLifecycleEffect(
                cameraController = cameraController,
                cameraPermissionGranted = cameraPermissionGranted,
                isCameraPreviewVisible = isCameraPreviewVisible,
                lifecycleOwner = lifecycleOwner,
            )
        }
    }
}
