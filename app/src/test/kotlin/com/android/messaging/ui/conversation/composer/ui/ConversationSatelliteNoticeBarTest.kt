package com.android.messaging.ui.conversation.composer.ui

import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.messaging.ui.conversation.CONVERSATION_SATELLITE_NOTICE_TEST_TAG
import com.android.messaging.ui.conversation.composer.model.ConversationSatelliteNotice
import com.android.messaging.ui.core.MessagingPreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationSatelliteNoticeBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noticeBar_keepsItsContentClearOfHorizontalSystemBarsAndCutouts() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            MessagingPreviewTheme {
                ConversationSatelliteNoticeBar(notice = ConversationSatelliteNotice.TextOnly)
            }
        }

        // Landscape with three-button navigation on the right and a cutout on the left.
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(LEFT_INSET, 0, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, RIGHT_INSET, 0))
            .build()
        composeTestRule.runOnIdle {
            ViewCompat.dispatchApplyWindowInsets(view, insets)
        }
        composeTestRule.waitForIdle()

        val bounds = composeTestRule
            .onNodeWithTag(CONVERSATION_SATELLITE_NOTICE_TEST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val contentPadding = with(composeTestRule.density) { 16.dp.toPx() }

        assertEquals(LEFT_INSET + contentPadding, bounds.left, 0.5f)
        assertEquals(view.width - RIGHT_INSET - contentPadding, bounds.right, 0.5f)
    }

    private companion object {
        private const val LEFT_INSET = 120
        private const val RIGHT_INSET = 96
    }
}
