package com.android.messaging.ui.conversation.messages.ui.message

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.messaging.R
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.conversationMessageSelectionRowTestTag
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLink
import com.android.messaging.ui.conversation.messages.model.text.ConversationTextLinkKind
import com.android.messaging.ui.conversation.messages.ui.text.ConversationTextSelectionCoordinator
import com.android.messaging.ui.conversation.messages.ui.text.ConversationTextSelectionHost
import com.android.messaging.ui.conversation.messages.ui.text.LocalConversationTextSelectionCoordinator
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.core.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationTextSelectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var coordinator: ConversationTextSelectionCoordinator? = null
    private var selectedMessageCount = 0
    private var openedLinkCount = 0
    private var openedLink: String? = null
    private var copiedText: String? = null

    @Test
    fun longPressPlainTextSelectsTextWithoutSelectingMessage() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        composeTestRule.runOnIdle {
            assertEquals(0, selectedMessageCount)
            assertNotNull(coordinator?.active)
            assertTrue(coordinator?.active?.hasSelection == true)
        }
    }

    @Test
    fun longPressLinkSelectsTheWholeLinkWithoutOpeningOrSelectingMessage() {
        showMessage(text = LINK_TEXT)
        awaitLink(text = LINK_TEXT)
        longPress(text = LINK_TEXT)
        composeTestRule.runOnIdle {
            assertEquals(0, selectedMessageCount)
            assertEquals(0, openedLinkCount)
            assertEquals(
                LINK_TEXT,
                coordinator?.active?.selection?.selectedTexts?.singleOrNull()?.text,
            )
            assertNotNull(coordinator?.active?.selectedLink)
        }
    }

    @Test
    fun longPressInsideAWrappedLinkSelectsItOverTheNearerLinkBelow() {
        val text = "$WRAPPED_LINK_TEXT $OTHER_LINK_TEXT"
        showMessage(text = text, modifier = Modifier.width(NARROW_SCREEN_WIDTH))
        val otherLinkStart = text.indexOf(string = OTHER_LINK_TEXT)
        awaitLink(text = text, start = otherLinkStart)
        val layout = textLayout(text = text)
        val linkLastLine = layout.getLineForOffset(offset = WRAPPED_LINK_TEXT.lastIndex)
        assertTrue("Link did not wrap", linkLastLine > 0)
        val lines = List(size = layout.lineCount) { line ->
            text.substring(
                layout.getLineStart(lineIndex = line),
                layout.getLineEnd(lineIndex = line),
            )
        }
        assertEquals(
            "The other link does not follow the link's last line: $lines",
            linkLastLine,
            layout.getLineForOffset(offset = otherLinkStart),
        )
        val pressedLine = linkLastLine - 1
        val pressedLineRight = layout.getLineRight(lineIndex = pressedLine)
        val otherLinkLeft = layout.getBoundingBox(offset = otherLinkStart).left
        val otherLinkLineEnd = minOf(
            layout.getLineEnd(lineIndex = linkLastLine, visibleEnd = true),
            otherLinkStart + OTHER_LINK_TEXT.length,
        )
        val otherLinkRight = layout.getBoundingBox(offset = otherLinkLineEnd - 1).right
        val position = Offset(
            x = (maxOf(pressedLineRight, otherLinkLeft) + otherLinkRight) / 2,
            y = layout.getLineBottom(lineIndex = pressedLine) - LINE_EDGE_INSET,
        )
        assertTrue(
            "The other link does not reach past the wrapped line's end: $lines",
            position.x - pressedLineRight > LINE_EDGE_INSET * 2,
        )
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true).performTouchInput {
            longClick(position = position)
        }
        composeTestRule.runOnIdle {
            assertEquals(0, openedLinkCount)
            assertEquals(WRAPPED_LINK_TEXT, coordinator?.active?.selectedLink?.url)
        }
    }

    @Test
    fun longPressBesideALinkSelectsTheLinkThatATapThereOpens() {
        val text = "$SHORT_LINK_TEXT\n$LONGER_LINK_TEXT"
        showMessage(text = text)
        awaitLink(text = text, start = text.indexOf(string = LONGER_LINK_TEXT))
        val shortLinkBounds = textLayout(text = text)
            .getPathForRange(start = 0, end = SHORT_LINK_TEXT.length)
            .getBounds()
        val besideShortLinkAboveLongerLink = Offset(
            x = shortLinkBounds.right + 1f,
            y = shortLinkBounds.bottom - LINE_EDGE_INSET,
        )
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true).performTouchInput {
            click(position = besideShortLinkAboveLongerLink)
            advanceEventTime(durationMillis = ViewConfiguration.getDoubleTapTimeout().toLong())
            longClick(position = besideShortLinkAboveLongerLink)
        }
        composeTestRule.runOnIdle {
            assertEquals(LONGER_LINK_TEXT, openedLink)
            assertEquals(LONGER_LINK_TEXT, coordinator?.active?.selectedLink?.url)
        }
    }

    @Test
    fun longPressOnTheLineBreakOfAWrappedLinkSelectsTheLink() {
        showMessage(text = LINK_TEXT, modifier = Modifier.width(NARROW_SCREEN_WIDTH))
        awaitLink(text = LINK_TEXT)
        val layout = textLayout(text = LINK_TEXT)
        val lastLine = layout.lineCount - 1
        val lastLineRight = layout.getLineRight(lineIndex = lastLine)
        val previousLineRight = layout.getLineRight(lineIndex = lastLine - 1)
        assertTrue("Link did not wrap onto a shorter line", lastLineRight < previousLineRight)
        val position = Offset(
            x = (lastLineRight + previousLineRight) / 2,
            y = layout.getLineTop(lineIndex = lastLine),
        )
        composeTestRule.onNodeWithText(text = LINK_TEXT, useUnmergedTree = true).performTouchInput {
            longClick(position = position)
        }
        composeTestRule.runOnIdle {
            assertEquals(0, openedLinkCount)
            assertEquals(LINK_TEXT, coordinator?.active?.selectedLink?.url)
        }
    }

    @Test
    fun firstLinkTapDismissesSelectionAndSecondTapOpensLink() {
        showMessage(text = LINK_TEXT)
        awaitLink(text = LINK_TEXT)
        longPress(text = LINK_TEXT)
        tap(text = LINK_TEXT)
        composeTestRule.runOnIdle {
            assertFalse(coordinator?.active?.hasSelection == true)
            assertEquals(0, openedLinkCount)
        }
        tap(text = LINK_TEXT, delayMillis = ViewConfiguration.getDoubleTapTimeout().toLong())
        composeTestRule.runOnIdle { assertEquals(1, openedLinkCount) }
    }

    @Test
    fun linkLongPressDragExtendsSelectionIntoFollowingText() {
        val text = "$LINK_TEXT more words"
        showMessage(text = text)
        awaitLink(text = text)
        val layout = textLayout(text = text)
        val start = layout.getBoundingBox(offset = LINK_TEXT.indexOf("example")).center
        val end = layout.getBoundingBox(offset = text.lastIndex).center
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true).performTouchInput {
            down(position = start)
            advanceEventTime(
                durationMillis = ViewConfiguration.getLongPressTimeout().toLong() + 100,
            )
            moveTo(position = end, delayMillis = 100)
            up()
        }
        composeTestRule.runOnIdle {
            val selected = coordinator?.active?.selection?.selectedTexts?.singleOrNull()?.text
            assertTrue("Selected: $selected", selected?.endsWith("more words") == true)
            assertEquals(0, openedLinkCount)
            assertEquals(0, selectedMessageCount)
        }
    }

    @Test
    fun plainTextMenuCopiesSelectedText() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        clickMenu(labelResourceId = android.R.string.copy)
        composeTestRule.runOnIdle { assertFalse(coordinator?.active?.hasSelection == true) }
    }

    @Test
    fun copyLinkMenuCopiesTheSelectedLink() {
        showMessage(text = LINK_TEXT)
        awaitLink(text = LINK_TEXT)
        longPress(text = LINK_TEXT)
        clickMenu(labelResourceId = R.string.conversation_copy_link)
        composeTestRule.runOnIdle {
            assertEquals(LINK_TEXT, copiedText)
            assertFalse(coordinator?.active?.hasSelection == true)
            assertEquals(0, openedLinkCount)
        }
    }

    private fun awaitLink(text: String, start: Int = 0) {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            val annotated = composeTestRule.onNodeWithText(text = text, useUnmergedTree = true)
                .fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.firstOrNull()
            annotated?.hasLinkAnnotations(start = start, end = annotated.length) == true
        }
    }

    @Test
    fun nativeHandleExtendsSelectionAfterLinkLongPress() {
        showMessage(text = LINK_TEXT)
        awaitLink(text = LINK_TEXT)
        longPress(text = LINK_TEXT)
        composeTestRule.onAllNodes(matcher = isPopup(), useUnmergedTree = true)[1]
            .performTouchInput {
                down(position = center)
                moveTo(position = center + Offset(x = 300f, y = 0f), delayMillis = 300)
                up()
            }
        composeTestRule.runOnIdle {
            val selected = coordinator?.active?.selection?.selectedTexts?.singleOrNull()?.text
            assertTrue("Selected: $selected", selected?.endsWith("/details") == true)
            assertEquals(0, openedLinkCount)
        }
    }

    @Test
    fun subjectAndBodyCopyExcludesSenderAndMetadata() {
        showMessage(text = PLAIN_TEXT, subject = "Subject")
        longPress(text = PLAIN_TEXT)
        composeTestRule.runOnIdle {
            val state = requireNotNull(coordinator?.active)
            state.selection.selectAll()
            assertEquals(
                listOf("Subject", PLAIN_TEXT),
                state.selection.selectedTexts.map { it.text },
            )
        }
        clickMenu(labelResourceId = android.R.string.copy)
        composeTestRule.runOnIdle {
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
                .getSystemService(ClipboardManager::class.java)
            assertEquals(
                "Subject\n$PLAIN_TEXT",
                clipboard.primaryClip?.getItemAt(0)?.text?.toString(),
            )
        }
    }

    @Test
    fun copyOfALinkUnderASubjectHasNoLeadingNewline() {
        showMessage(text = LINK_TEXT, subject = "Subject")
        awaitLink(text = LINK_TEXT)
        longPress(text = LINK_TEXT)
        clickMenu(labelResourceId = android.R.string.copy)
        composeTestRule.runOnIdle {
            val clipboard = InstrumentationRegistry.getInstrumentation().targetContext
                .getSystemService(ClipboardManager::class.java)
            assertEquals(LINK_TEXT, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test
    fun linkLongPressDragCanReachTheSubject() {
        showMessage(text = LINK_TEXT, subject = "Subject")
        awaitLink(text = LINK_TEXT)
        val layout = textLayout(text = LINK_TEXT)
        val bodyBounds = composeTestRule.onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val subjectBounds = composeTestRule.onNodeWithText(text = "Subject", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val start = layout.getBoundingBox(offset = LINK_TEXT.indexOf("example")).center
        val end = subjectBounds.center - bodyBounds.topLeft
        composeTestRule.onNodeWithText(text = LINK_TEXT, useUnmergedTree = true).performTouchInput {
            down(position = start)
            advanceEventTime(
                durationMillis = ViewConfiguration.getLongPressTimeout().toLong() + 100,
            )
            moveTo(position = end, delayMillis = 100)
            up()
        }
        composeTestRule.runOnIdle {
            val selected = requireNotNull(coordinator?.active).selection.selectedTexts
            assertEquals("Subject", selected.first().text)
            assertTrue(selected.last().text.startsWith("https://example"))
            assertEquals(0, openedLinkCount)
        }
    }

    @Test
    fun selectingMessageClearsTextSelection() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        val state = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        clickMenu(labelResourceId = R.string.conversation_message_select_message)
        composeTestRule.runOnIdle {
            assertFalse(state.hasSelection)
            assertEquals(1, selectedMessageCount)
        }
    }

    @Test
    fun longPressBesideTheBubbleSelectsTheMessageWithoutSelectingText() {
        showMessage(text = PLAIN_TEXT)
        longPressBesideBubble(text = PLAIN_TEXT)
        composeTestRule.runOnIdle {
            assertEquals(1, selectedMessageCount)
            assertFalse(coordinator?.active?.hasSelection == true)
        }
    }

    @Test
    fun longPressBesideTheBubbleClearsTheTextSelection() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        val state = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        longPressBesideBubble(text = PLAIN_TEXT)
        composeTestRule.runOnIdle {
            assertFalse(state.hasSelection)
            assertEquals(1, selectedMessageCount)
        }
    }

    @Test
    fun copyLinkMenuTracksExactlyOneSelectedOccurrence() {
        val second = "https://example.org"
        val text = "$LINK_TEXT and $second"
        showMessage(text = text)
        awaitLink(text = text)
        val layout = textLayout(text = text)
        val position = layout.getBoundingBox(offset = 10).center
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true).performTouchInput {
            longClick(position = position)
        }
        awaitMenuItem(labelResourceId = R.string.conversation_copy_link)
        val state = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        composeTestRule.runOnIdle {
            state.selection.selectAll()
            assertNull(state.selectedLink)
        }
        awaitMenuItem(labelResourceId = android.R.string.copy)
        awaitNoMenuItem(labelResourceId = R.string.conversation_copy_link)
        composeTestRule.runOnIdle {
            val start = text.indexOf(string = second) + 8
            state.selection.select(range = TextRange(start = start, end = start + 3))
        }
        clickMenu(labelResourceId = R.string.conversation_copy_link)
        composeTestRule.runOnIdle {
            assertEquals(second, copiedText)
        }
    }

    @Test
    fun dismissClearsSelectionWithoutSelectingMessage() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        composeTestRule.runOnIdle {
            val state = requireNotNull(coordinator?.active)
            assertTrue(coordinator?.dismiss() == true)
            assertFalse(state.hasSelection)
            assertEquals(0, selectedMessageCount)
        }
    }

    @Test
    fun dismissClearsASelectionStartedBeforeItsMessageBecameActive() {
        showTwoMessages()
        longPress(text = SECOND_MESSAGE_TEXT)
        val second = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        longPress(text = PLAIN_TEXT)
        val first = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        composeTestRule.runOnIdle {
            second.selection.select(range = TextRange(start = 0, end = 6))
            assertTrue(second.hasSelection)
            assertTrue(coordinator?.dismiss() == true)
            assertFalse(first.hasSelection)
            assertFalse(second.hasSelection)
        }
    }

    @Test
    fun scrollingTheSelectedMessageOutOfViewClearsTheSelection() {
        val messages = List(size = 40) { index ->
            previewIncomingMessage(messageId = MessageId("message-$index"), text = "Message $index")
        }
        composeTestRule.setContent {
            AppTheme {
                ConversationTextSelectionHost(
                    conversationId = messages.first().conversationId,
                    onCopyText = { copiedText = it },
                ) {
                    coordinator = LocalConversationTextSelectionCoordinator.current
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp),
                    ) {
                        items(items = messages, key = { it.messageId.value }) { message ->
                            ConversationMessage(message = message)
                        }
                    }
                }
            }
        }
        longPress(text = "Message 0")
        val state = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        composeTestRule.onNode(matcher = hasScrollToIndexAction())
            .performScrollToIndex(index = messages.lastIndex)
        composeTestRule.runOnIdle {
            assertFalse(state.hasSelection)
            assertNull(coordinator?.active)
        }
    }

    @Test
    fun doubleTapDismissesTextSelectionWithoutSelectingAWord() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        doubleTap(text = PLAIN_TEXT)
        composeTestRule.runOnIdle {
            assertFalse(coordinator?.hasSelection == true)
            assertEquals(0, selectedMessageCount)
        }
    }

    @Test
    fun doubleTapWithoutTextSelectionSelectsText() {
        showMessage(text = PLAIN_TEXT)
        doubleTap(text = PLAIN_TEXT)
        composeTestRule.runOnIdle { assertTrue(coordinator?.active?.hasSelection == true) }
    }

    @Test
    fun backDismissesTextSelection() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        pressBack()
        composeTestRule.runOnIdle {
            assertFalse(coordinator?.active?.hasSelection == true)
            assertEquals(0, selectedMessageCount)
        }
    }

    @Test
    fun selectingAnotherMessageClearsThePreviousRange() {
        showTwoMessages()
        longPress(text = PLAIN_TEXT)
        val previous = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        longPress(text = SECOND_MESSAGE_TEXT)
        composeTestRule.runOnIdle {
            assertFalse(previous.hasSelection)
            assertTrue(coordinator?.active?.hasSelection == true)
            assertTrue(coordinator?.active !== previous)
        }
    }

    @Test
    fun changingMessageContentClearsTheSelectedRange() {
        var body by mutableStateOf(PLAIN_TEXT)
        val message = previewIncomingMessage(text = PLAIN_TEXT)
        composeTestRule.setContent {
            AppTheme {
                ConversationTextSelectionHost(
                    conversationId = message.conversationId,
                    onCopyText = { copiedText = it },
                ) {
                    coordinator = LocalConversationTextSelectionCoordinator.current
                    ConversationMessage(message = message.copy(text = body))
                }
            }
        }
        longPress(text = PLAIN_TEXT)
        composeTestRule.runOnIdle { body = "Updated message" }
        composeTestRule.runOnIdle { assertFalse(coordinator?.active?.hasSelection == true) }
    }

    @Test
    fun lateLinkDetectionWaitsUntilSelectionIsDismissed() {
        showMessage(text = PLAIN_TEXT)
        longPress(text = PLAIN_TEXT)
        val link = ConversationTextLink(
            start = 6,
            end = 11,
            url = "http://example.com",
            kind = ConversationTextLinkKind.Web,
            originalText = "bravo",
        )
        val state = composeTestRule.runOnIdle { requireNotNull(coordinator?.active) }
        val selected = composeTestRule.runOnIdle { state.selection.selectedTexts }
        composeTestRule.runOnIdle { state.updateLinks(detectedLinks = listOf(link)) }
        composeTestRule.runOnIdle {
            assertTrue(state.links.isEmpty())
            assertEquals(selected, state.selection.selectedTexts)
            state.selection.clear()
        }
        composeTestRule.runOnIdle { assertEquals(listOf(link), state.links) }
    }

    @Test
    fun selectedLinkSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(composeTestRule)
        val message = previewIncomingMessage(text = LINK_TEXT)
        restoration.setContent {
            AppTheme {
                ConversationTextSelectionHost(
                    conversationId = message.conversationId,
                    onCopyText = { copiedText = it },
                ) {
                    coordinator = LocalConversationTextSelectionCoordinator.current
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp)
                    ) {
                        ConversationMessage(message = message)
                    }
                }
            }
        }
        awaitLink(text = LINK_TEXT)
        longPress(text = LINK_TEXT)
        restoration.emulateSavedInstanceStateRestore()
        composeTestRule.runOnIdle {
            assertEquals(LINK_TEXT, coordinator?.active?.selectedLink?.url)
        }
        composeTestRule.onAllNodes(matcher = isPopup(), useUnmergedTree = true).assertCountEquals(2)
        composeTestRule.onAllNodes(matcher = isPopup(), useUnmergedTree = true)[1]
            .performTouchInput {
                down(position = center)
                moveTo(position = center + Offset(x = 300f, y = 0f), delayMillis = 300)
                up()
            }
        clickMenu(labelResourceId = R.string.conversation_copy_link)
        composeTestRule.runOnIdle {
            assertEquals(LINK_TEXT, copiedText)
        }
    }

    @Test
    fun selectedRangeSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(composeTestRule)
        val message = previewIncomingMessage(text = PLAIN_TEXT)
        restoration.setContent {
            AppTheme {
                ConversationTextSelectionHost(
                    conversationId = message.conversationId,
                    onCopyText = { copiedText = it },
                ) {
                    coordinator = LocalConversationTextSelectionCoordinator.current
                    ConversationMessage(message = message)
                }
            }
        }
        longPress(text = PLAIN_TEXT)
        composeTestRule.runOnIdle {
            val state = requireNotNull(coordinator?.active)
            state.selection.select(range = TextRange(start = 6, end = 11))
        }
        restoration.emulateSavedInstanceStateRestore()
        composeTestRule.runOnIdle {
            assertEquals(
                "bravo",
                coordinator?.active?.selection?.selectedTexts?.singleOrNull()?.text,
            )
        }
        composeTestRule.onAllNodes(matcher = isPopup(), useUnmergedTree = true).assertCountEquals(2)
    }

    private fun showMessage(
        text: String,
        subject: String? = null,
        modifier: Modifier = Modifier,
    ) {
        val message = previewIncomingMessage(text = text).copy(mmsSubject = subject)
        composeTestRule.setContent {
            AppTheme {
                ConversationTextSelectionHost(
                    conversationId = message.conversationId,
                    onCopyText = { copiedText = it },
                ) {
                    coordinator = LocalConversationTextSelectionCoordinator.current
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp)
                    ) {
                        ConversationMessage(
                            modifier = modifier,
                            message = message,
                            onMessageLongClick = { selectedMessageCount += 1 },
                            onExternalUriClick = { uri ->
                                openedLinkCount += 1
                                openedLink = uri
                            },
                        )
                    }
                }
            }
        }
    }

    private fun showTwoMessages() {
        val first = previewIncomingMessage(messageId = MessageId("first"), text = PLAIN_TEXT)
        val second =
            previewIncomingMessage(messageId = MessageId("second"), text = SECOND_MESSAGE_TEXT)
        composeTestRule.setContent {
            AppTheme {
                ConversationTextSelectionHost(
                    conversationId = first.conversationId,
                    onCopyText = { copiedText = it },
                ) {
                    coordinator = LocalConversationTextSelectionCoordinator.current
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 120.dp)
                    ) {
                        ConversationMessage(message = first)
                        ConversationMessage(message = second)
                    }
                }
            }
        }
    }

    private fun longPress(text: String) {
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true).performTouchInput {
            longClick(position = center)
        }
        composeTestRule.waitForIdle()
    }

    private fun longPressBesideBubble(text: String) {
        val textBounds = composeTestRule.onNodeWithText(text = text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val row = composeTestRule.onNodeWithTag(
            testTag = conversationMessageSelectionRowTestTag(
                messageId = previewIncomingMessage().messageId,
            ),
            useUnmergedTree = true,
        )
        val rowBounds = row.fetchSemanticsNode().boundsInRoot
        row.performTouchInput {
            longClick(
                position = Offset(
                    x = width - 24.dp.toPx(),
                    y = textBounds.center.y - rowBounds.top,
                ),
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun tap(text: String, delayMillis: Long = 0) {
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true).performTouchInput {
            advanceEventTime(durationMillis = delayMillis)
            click(position = center)
        }
        composeTestRule.waitForIdle()
    }

    private fun doubleTap(text: String) {
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true).performTouchInput {
            doubleClick(position = center)
        }
        composeTestRule.waitForIdle()
    }

    private fun textLayout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(text = text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    private fun clickMenu(labelResourceId: Int) {
        val menuItem = awaitMenuItem(labelResourceId = labelResourceId)
        val clicked = menuItem.performAction(AccessibilityNodeInfo.ACTION_CLICK) ||
            menuItem.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        assertTrue("Could not click menu item $labelResourceId", clicked)
        composeTestRule.waitForIdle()
    }

    /**
     * Looks for the item and the overflow button in one snapshot: separate lookups can straddle
     * the toolbar appearing and open the overflow, which hides an item that is in the toolbar.
     */
    private fun awaitMenuItem(labelResourceId: Int): AccessibilityNodeInfo {
        retrieveInteractiveWindows()
        val label = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(labelResourceId)
        val openOverflowLabel = floatingToolbarLabel(name = OPEN_OVERFLOW_DESCRIPTION)
        var item: AccessibilityNodeInfo? = null
        var overflowOpened = false
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.waitForIdle()
            val nodes = visibleNodes()
            item = nodes.withLabel(label = label)
            if (item == null && !overflowOpened) {
                overflowOpened = nodes.withLabel(label = openOverflowLabel)
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            }
            item != null
        }
        return requireNotNull(item)
    }

    /** Waits until neither the toolbar nor its overflow, which show one at a time, has the item. */
    private fun awaitNoMenuItem(labelResourceId: Int) {
        retrieveInteractiveWindows()
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.waitForIdle()
            findMenuItem(labelResourceId = labelResourceId) == null &&
                findNode(label = floatingToolbarLabel(name = CLOSE_OVERFLOW_DESCRIPTION)) == null
        }
        val overflowButton = findNode(
            label = floatingToolbarLabel(name = OPEN_OVERFLOW_DESCRIPTION),
        ) ?: return
        overflowButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            findNode(label = floatingToolbarLabel(name = CLOSE_OVERFLOW_DESCRIPTION)) != null
        }
        assertNull(findMenuItem(labelResourceId = labelResourceId))
    }

    private fun retrieveInteractiveWindows() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val serviceInfo = automation.serviceInfo
        serviceInfo.flags =
            serviceInfo.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = serviceInfo
    }

    private fun findMenuItem(labelResourceId: Int): AccessibilityNodeInfo? {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return findNode(label = context.getString(labelResourceId))
    }

    private fun findNode(label: String): AccessibilityNodeInfo? {
        return visibleNodes().withLabel(label = label)
    }

    private fun visibleNodes(): List<AccessibilityNodeInfo> {
        val nodes = ArrayDeque<AccessibilityNodeInfo>()
        InstrumentationRegistry.getInstrumentation().uiAutomation.windows
            .mapNotNull { it.root }
            .forEach { nodes.add(it) }
        val visibleNodes = mutableListOf<AccessibilityNodeInfo>()
        while (nodes.isNotEmpty()) {
            val node = nodes.removeFirst()
            if (node.isVisibleToUser) {
                visibleNodes.add(node)
            }
            repeat(node.childCount) { node.getChild(it)?.let { child -> nodes.add(child) } }
        }
        return visibleNodes
    }

    private fun List<AccessibilityNodeInfo>.withLabel(label: String): AccessibilityNodeInfo? {
        return firstOrNull {
            it.text?.toString() == label || it.contentDescription?.toString() == label
        }
    }

    /** The platform's floating text toolbar labels its overflow button with these strings. */
    private fun floatingToolbarLabel(name: String): String {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        return resources.getString(resources.getIdentifier(name, "string", "android"))
    }

    companion object {
        private const val PLAIN_TEXT = "Alpha bravo charlie delta"
        private const val LINK_TEXT = "https://example.com/details"
        private const val WRAPPED_LINK_TEXT = "https://ab.io/details/abcdefghijk"
        private const val OTHER_LINK_TEXT = "me@ab.io"
        private const val SHORT_LINK_TEXT = "https://ab.io"
        private const val LONGER_LINK_TEXT = "https://ab.io/details"
        private const val SECOND_MESSAGE_TEXT = "Second message"
        private const val LINE_EDGE_INSET = 3f
        private const val OPEN_OVERFLOW_DESCRIPTION = "floating_toolbar_open_overflow_description"
        private const val CLOSE_OVERFLOW_DESCRIPTION = "floating_toolbar_close_overflow_description"
        private val NARROW_SCREEN_WIDTH = 320.dp
    }
}
