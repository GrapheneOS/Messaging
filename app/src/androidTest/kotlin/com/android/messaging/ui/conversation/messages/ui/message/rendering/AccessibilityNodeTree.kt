package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry

private const val A11Y_TREE_POLL_ATTEMPTS = 20
private const val A11Y_TREE_POLL_INTERVAL_MILLIS = 250L

/** The nodes TalkBack would stop on and read [text] out from. */
internal fun awaitFocusableNodesSpeaking(text: String): List<AccessibilityNodeInfo> {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    repeat(A11Y_TREE_POLL_ATTEMPTS) {
        val matches = uiAutomation.rootInActiveWindow?.collectNodes().orEmpty().filter { node ->
            node.isScreenReaderFocusable && node.subtreeText().contains(text)
        }
        if (matches.isNotEmpty()) {
            return matches
        }
        Thread.sleep(A11Y_TREE_POLL_INTERVAL_MILLIS)
    }
    return emptyList()
}

internal fun dumpActiveWindow(): String {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    return uiAutomation.rootInActiveWindow?.dumpSubtree() ?: "<no active window>"
}

internal fun AccessibilityNodeInfo.subtreeText(): String {
    return collectNodes().joinToString(separator = " ") { node ->
        listOfNotNull(node.text, node.contentDescription).joinToString(separator = " ")
    }
}

internal fun AccessibilityNodeInfo.hasDescendant(node: AccessibilityNodeInfo): Boolean {
    return collectNodes().drop(n = 1).contains(node)
}

internal fun AccessibilityNodeInfo.dumpSubtree(): String {
    return collectNodes().joinToString(separator = " / ") { it.describe() }
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
