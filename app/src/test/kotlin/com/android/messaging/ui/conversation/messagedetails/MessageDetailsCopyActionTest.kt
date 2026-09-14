package com.android.messaging.ui.conversation.messagedetails

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.ui.core.AppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MessageDetailsCopyActionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        installTestFactory(context = targetContext)
    }

    @After
    fun tearDown() {
        FactoryTestAccess.reset()
    }

    @Test
    fun copyActionIsOfferedOnTheNodeAScreenReaderFocuses() {
        val copied = mutableListOf<String>()

        composeTestRule.setContent {
            AppTheme {
                MessageDetailsStatusSection(
                    sentTimestamp = null,
                    receivedTimestamp = RECEIVED_TIMESTAMP,
                    onCopy = { copied += it },
                )
            }
        }

        val label = targetContext.getString(R.string.message_details_received_label)
        val focusedNode = composeTestRule.onNodeWithText(label).fetchSemanticsNode()

        val customActions = focusedNode.config.getOrNull(SemanticsActions.CustomActions)
        assertNotNull("No custom action on the node holding the row text", customActions)

        assertEquals(
            targetContext.getString(R.string.copy_to_clipboard),
            customActions!!.single().label,
        )

        customActions.single().action()

        assertEquals(
            listOf(formatMessageDetailsTimestamp(timestampMillis = RECEIVED_TIMESTAMP)),
            copied,
        )
    }

    private companion object {
        private const val RECEIVED_TIMESTAMP = 1_700_000_000_000L
    }
}
