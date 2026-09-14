package com.android.messaging.ui.conversation.messagedetails

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class MessageDetailsCopyActionA11yTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun copyActionSitsOnAScreenReaderFocusableNode() {
        composeTestRule.setContent {
            AppTheme {
                MessageDetailsStatusSection(
                    sentTimestamp = null,
                    receivedTimestamp = RECEIVED_TIMESTAMP,
                    onCopy = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val copyLabel = context.getString(R.string.copy_to_clipboard)
        val receivedLabel = context.getString(R.string.message_details_received_label)

        val nodesOfferingCopy = awaitNodesOfferingCopy(copyLabel = copyLabel)

        assertTrue(
            "No accessibility node exposes the \"$copyLabel\" custom action at all",
            nodesOfferingCopy.isNotEmpty(),
        )

        val focusableNodes = nodesOfferingCopy.filter { it.isScreenReaderFocusable }

        assertTrue(
            "The \"$copyLabel\" action exists on ${nodesOfferingCopy.size} node(s) but none is " +
                "screen reader focusable, so TalkBack never offers it: " +
                nodesOfferingCopy.joinToString { it.describe() },
            focusableNodes.isNotEmpty(),
        )

        val receivedValue = formatMessageDetailsTimestamp(timestampMillis = RECEIVED_TIMESTAMP)

        assertTrue(
            "The focusable node offering \"$copyLabel\" announces nothing about the row it " +
                "copies: " + focusableNodes.joinToString { it.dumpSubtree() },
            focusableNodes.any { node ->
                node.subtreeText().let {
                    it.contains(receivedLabel) && it.contains(receivedValue)
                }
            },
        )
    }

    private fun AccessibilityNodeInfo.subtreeText(): String {
        return collectNodes().joinToString(separator = " ") { node ->
            listOfNotNull(node.text, node.contentDescription).joinToString(separator = " ")
        }
    }

    private fun AccessibilityNodeInfo.dumpSubtree(): String {
        return collectNodes().joinToString(separator = " / ") { it.describe() }
    }

    private fun awaitNodesOfferingCopy(copyLabel: String): List<AccessibilityNodeInfo> {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(A11Y_TREE_POLL_ATTEMPTS) {
            val root = uiAutomation.rootInActiveWindow
            val matches = root?.collectNodes().orEmpty().filter { node ->
                node.actionList.any { it.label == copyLabel }
            }
            if (matches.isNotEmpty()) {
                return matches
            }
            Thread.sleep(A11Y_TREE_POLL_INTERVAL_MILLIS)
        }
        return emptyList()
    }

    private fun AccessibilityNodeInfo.collectNodes(): List<AccessibilityNodeInfo> {
        return buildList {
            add(this@collectNodes)
            repeat(childCount) { index ->
                getChild(index)?.let { addAll(it.collectNodes()) }
            }
        }
    }

    private fun AccessibilityNodeInfo.describe(): String {
        return "[text=$text, contentDescription=$contentDescription, " +
            "screenReaderFocusable=$isScreenReaderFocusable, " +
            "actions=${actionList.mapNotNull { it.label }}]"
    }

    private companion object {
        private const val RECEIVED_TIMESTAMP = 1_700_000_000_000L
        private const val A11Y_TREE_POLL_ATTEMPTS = 20
        private const val A11Y_TREE_POLL_INTERVAL_MILLIS = 250L
    }
}
