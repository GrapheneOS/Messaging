package com.android.messaging.ui.conversation.screen.route

import android.Manifest
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.messaging.ui.conversation.mediapicker.model.ConversationMediaPickerPermissionState
import com.android.messaging.ui.conversation.screen.AudioRecordingStartMode
import com.android.messaging.ui.conversation.screen.BaseConversationScreenTest
import com.android.messaging.ui.conversation.screen.ConversationScreenModel
import com.android.messaging.ui.conversation.screen.rememberAudioRecordingStartRequest
import com.android.messaging.ui.conversation.screen.rememberOpenContactPickerCallback
import com.android.messaging.ui.core.AppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationScreenRouteCallbackTest : BaseConversationScreenTest() {

    @Test
    fun audioRecordingStart_whenPermissionGrantedStartsRequestedMode() {
        val screenModel = createScreenModel().model
        every { screenModel.tryStartAddingAttachment() } returns true
        setAudioRecordingStartContent(
            screenModel = screenModel,
            audioPermissionGranted = true,
        )

        composeTestRule
            .onNodeWithTag(START_AUDIO_UNLOCKED_BUTTON_TAG)
            .performClick()
        composeTestRule
            .onNodeWithTag(START_AUDIO_LOCKED_BUTTON_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 2) {
                screenModel.tryStartAddingAttachment()
            }
            verify(exactly = 1) {
                screenModel.onAudioRecordingStart()
            }
            verify(exactly = 1) {
                screenModel.onLockedAudioRecordingStart()
            }
        }
    }

    @Test
    fun audioRecordingStart_whenAttachmentCannotStartDoesNotStartRecording() {
        val screenModel = createScreenModel().model
        every { screenModel.tryStartAddingAttachment() } returns false
        setAudioRecordingStartContent(
            screenModel = screenModel,
            audioPermissionGranted = true,
        )

        composeTestRule
            .onNodeWithTag(START_AUDIO_UNLOCKED_BUTTON_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.tryStartAddingAttachment()
            }
            verify(exactly = 0) {
                screenModel.onAudioRecordingStart()
            }
            verify(exactly = 0) {
                screenModel.onLockedAudioRecordingStart()
            }
        }
    }

    @Test
    fun audioRecordingStart_whenPermissionMissingRequestsPermissionBeforeStartingRecording() {
        val screenModel = createScreenModel().model
        val permissionLauncher = mockk<ActivityResultLauncher<String>>(relaxed = true)
        val permissionCallbackSlot = slot<ActivityResultCallback<Boolean>>()
        val registry = mockk<ActivityResultRegistry>()
        every { screenModel.tryStartAddingAttachment() } returns true
        every {
            registry.register(
                any<String>(),
                any<ActivityResultContract<String, Boolean>>(),
                capture(permissionCallbackSlot),
            )
        } returns permissionLauncher
        setAudioRecordingStartContent(
            screenModel = screenModel,
            audioPermissionGranted = false,
            activityResultRegistryOwner = createActivityResultRegistryOwner(
                activityResultRegistry = registry,
            ),
        )

        composeTestRule
            .onNodeWithTag(START_AUDIO_UNLOCKED_BUTTON_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO, null)
            }
            verify(exactly = 0) {
                screenModel.onAudioRecordingStart()
            }
            permissionCallbackSlot.captured.onActivityResult(true)
        }

        composeTestRule
            .onNodeWithTag(START_AUDIO_UNLOCKED_BUTTON_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 2) {
                screenModel.tryStartAddingAttachment()
            }
            verify(exactly = 1) {
                screenModel.onAudioRecordingStart()
            }
        }
    }

    @Test
    fun contactPicker_whenAttachmentCannotStartDoesNotLaunchPicker() {
        val screenModel = createScreenModel().model
        val contactLauncher = mockk<ActivityResultLauncher<Void?>>(relaxed = true)
        val contactCallbackSlot = slot<ActivityResultCallback<Uri?>>()
        val registry = mockk<ActivityResultRegistry>()
        every { screenModel.tryStartAddingAttachment() } returns false
        every {
            registry.register(
                any<String>(),
                any<ActivityResultContract<Void?, Uri?>>(),
                capture(contactCallbackSlot),
            )
        } returns contactLauncher
        setContactPickerContent(
            screenModel = screenModel,
            activityResultRegistryOwner = createActivityResultRegistryOwner(
                activityResultRegistry = registry,
            ),
        )

        composeTestRule
            .onNodeWithTag(OPEN_CONTACT_PICKER_BUTTON_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.tryStartAddingAttachment()
            }
            verify(exactly = 0) {
                contactLauncher.launch(any(), any())
            }
            verify(exactly = 0) {
                screenModel.onContactCardPicked(any())
            }
        }
    }

    @Test
    fun contactPicker_whenContactSelectedLaunchesPickerAndForwardsUri() {
        val screenModel = createScreenModel().model
        val contactLauncher = mockk<ActivityResultLauncher<Void?>>(relaxed = true)
        val contactCallbackSlot = slot<ActivityResultCallback<Uri?>>()
        val registry = mockk<ActivityResultRegistry>()
        every { screenModel.tryStartAddingAttachment() } returns true
        every {
            registry.register(
                any<String>(),
                any<ActivityResultContract<Void?, Uri?>>(),
                capture(contactCallbackSlot),
            )
        } returns contactLauncher
        setContactPickerContent(
            screenModel = screenModel,
            activityResultRegistryOwner = createActivityResultRegistryOwner(
                activityResultRegistry = registry,
            ),
        )

        composeTestRule
            .onNodeWithTag(OPEN_CONTACT_PICKER_BUTTON_TAG)
            .performClick()
        composeTestRule.runOnIdle {
            contactCallbackSlot.captured.onActivityResult(Uri.parse(CONTACT_URI))
        }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                contactLauncher.launch(null, null)
            }
            verify(exactly = 1) {
                screenModel.onContactCardPicked(contactUri = CONTACT_URI)
            }
        }
    }

    @Test
    fun contactPicker_whenPickerCanceledForwardsNullUri() {
        val screenModel = createScreenModel().model
        val contactLauncher = mockk<ActivityResultLauncher<Void?>>(relaxed = true)
        val contactCallbackSlot = slot<ActivityResultCallback<Uri?>>()
        val registry = mockk<ActivityResultRegistry>()
        every { screenModel.tryStartAddingAttachment() } returns true
        every {
            registry.register(
                any<String>(),
                any<ActivityResultContract<Void?, Uri?>>(),
                capture(contactCallbackSlot),
            )
        } returns contactLauncher
        setContactPickerContent(
            screenModel = screenModel,
            activityResultRegistryOwner = createActivityResultRegistryOwner(
                activityResultRegistry = registry,
            ),
        )

        composeTestRule
            .onNodeWithTag(OPEN_CONTACT_PICKER_BUTTON_TAG)
            .performClick()
        composeTestRule.runOnIdle {
            contactCallbackSlot.captured.onActivityResult(null)
        }

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                contactLauncher.launch(null, null)
            }
            verify(exactly = 1) {
                screenModel.onContactCardPicked(contactUri = null)
            }
        }
    }

    private fun setAudioRecordingStartContent(
        screenModel: ConversationScreenModel,
        audioPermissionGranted: Boolean,
        activityResultRegistryOwner: ActivityResultRegistryOwner? = null,
    ) {
        composeTestRule.setContent {
            val content: @Composable () -> Unit = {
                val context = LocalContext.current
                val permissionState = remember {
                    ConversationMediaPickerPermissionState(context = context).apply {
                        this.audioPermissionGranted = audioPermissionGranted
                    }
                }
                val startRequest = rememberAudioRecordingStartRequest(
                    screenModel = screenModel,
                    permissionState = permissionState,
                )

                Column {
                    Button(
                        modifier = Modifier.testTag(tag = START_AUDIO_UNLOCKED_BUTTON_TAG),
                        onClick = {
                            startRequest(AudioRecordingStartMode.Unlocked)
                        },
                    ) {
                        Text(text = "Unlocked")
                    }
                    Button(
                        modifier = Modifier.testTag(tag = START_AUDIO_LOCKED_BUTTON_TAG),
                        onClick = {
                            startRequest(AudioRecordingStartMode.Locked)
                        },
                    ) {
                        Text(text = "Locked")
                    }
                }
            }

            when (activityResultRegistryOwner) {
                null -> AppTheme(content = content)
                else -> {
                    CompositionLocalProvider(
                        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
                    ) {
                        AppTheme(content = content)
                    }
                }
            }
        }
    }

    private fun setContactPickerContent(
        screenModel: ConversationScreenModel,
        activityResultRegistryOwner: ActivityResultRegistryOwner,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
            ) {
                AppTheme {
                    val openContactPicker = rememberOpenContactPickerCallback(
                        screenModel = screenModel,
                    )

                    Button(
                        modifier = Modifier.testTag(tag = OPEN_CONTACT_PICKER_BUTTON_TAG),
                        onClick = openContactPicker,
                    ) {
                        Text(text = "Contact")
                    }
                }
            }
        }
    }

    private fun createActivityResultRegistryOwner(
        activityResultRegistry: ActivityResultRegistry,
    ): ActivityResultRegistryOwner {
        val owner = mockk<ActivityResultRegistryOwner>()
        every { owner.activityResultRegistry } returns activityResultRegistry
        return owner
    }

    private companion object {
        private const val START_AUDIO_UNLOCKED_BUTTON_TAG = "start_audio_unlocked"
        private const val START_AUDIO_LOCKED_BUTTON_TAG = "start_audio_locked"
        private const val OPEN_CONTACT_PICKER_BUTTON_TAG = "open_contact_picker"
        private const val CONTACT_URI = "content://contacts/people/11"
    }
}
