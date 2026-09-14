package com.android.messaging.ui.conversation.mediapicker.component.shared

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.android.messaging.ui.conversation.mediapicker.component.PermissionFallback
import com.android.messaging.ui.conversation.mediapicker.component.PickerOverlayBackgroundButton
import com.android.messaging.ui.conversation.mediapicker.component.PickerOverlayIconButton
import com.android.messaging.ui.core.AppTheme
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMediaPickerSharedControlsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onActionClick = mockk<() -> Unit>(relaxed = true)
    private val onButtonClick = mockk<() -> Unit>(relaxed = true)

    @Before
    fun setUpConversationMediaPickerSharedControlsTest() {
        clearAllMocks()
    }

    @Test
    fun permissionFallback_rendersMessageActionAndForwardsClick() {
        setPermissionFallbackContent()

        composeTestRule.onNodeWithText(PERMISSION_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText(PERMISSION_ACTION).assertIsDisplayed()

        composeTestRule
            .onNodeWithText(PERMISSION_ACTION)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onActionClick.invoke()
            }
        }
    }

    @Test
    fun overlayBackgroundButton_forwardsClick() {
        setBackgroundButtonContent()

        composeTestRule
            .onNodeWithContentDescription(BACKGROUND_BUTTON_DESCRIPTION)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onButtonClick.invoke()
            }
        }
    }

    @Test
    fun overlayIconButton_enabledForwardsClick() {
        setIconButtonContent(enabled = true)

        composeTestRule
            .onNodeWithContentDescription(ICON_BUTTON_DESCRIPTION)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                onButtonClick.invoke()
            }
        }
    }

    @Test
    fun overlayIconButton_disabledIgnoresClick() {
        setIconButtonContent(enabled = false)

        composeTestRule
            .onNodeWithContentDescription(ICON_BUTTON_DESCRIPTION)
            .assertIsNotEnabled()
            .performTouchInput {
                click(position = center)
            }

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onButtonClick.invoke()
            }
        }
    }

    private fun setPermissionFallbackContent() {
        composeTestRule.setContent {
            AppTheme {
                PermissionFallback(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                        )
                    },
                    message = PERMISSION_MESSAGE,
                    actionLabel = PERMISSION_ACTION,
                    onActionClick = onActionClick,
                )
            }
        }
    }

    private fun setBackgroundButtonContent() {
        composeTestRule.setContent {
            AppTheme {
                PickerOverlayBackgroundButton(
                    contentDescription = BACKGROUND_BUTTON_DESCRIPTION,
                    imageVector = Icons.Rounded.Close,
                    onClick = onButtonClick,
                )
            }
        }
    }

    private fun setIconButtonContent(enabled: Boolean) {
        composeTestRule.setContent {
            AppTheme {
                PickerOverlayIconButton(
                    contentDescription = ICON_BUTTON_DESCRIPTION,
                    enabled = enabled,
                    imageVector = Icons.Rounded.Close,
                    onClick = onButtonClick,
                )
            }
        }
    }

    private companion object {
        private const val PERMISSION_MESSAGE = "Camera access is required"
        private const val PERMISSION_ACTION = "Allow camera"
        private const val BACKGROUND_BUTTON_DESCRIPTION = "Background button"
        private const val ICON_BUTTON_DESCRIPTION = "Icon button"
    }
}
