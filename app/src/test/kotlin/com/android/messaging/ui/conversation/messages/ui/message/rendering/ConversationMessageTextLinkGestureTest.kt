package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.view.ViewConfiguration
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSeparator
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.data.appsettings.model.LinkOpeningMode
import com.android.messaging.testutil.TEST_WAIT_TIMEOUT_MILLIS
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMessageTextLinkGestureTest : BaseConversationMessageRenderingTest() {

    private val textContextMenuToolbar = RecordingTextContextMenuToolbar()

    @Volatile
    private var classifiedSelectionCount = 0

    @Before
    fun classifyLinksByPattern() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val openLinkAction = openLinkAction(context = context)
        context
            .getSystemService(TextClassificationManager::class.java)
            .setTextClassifier(
                object : TextClassifier {
                    override fun classifyText(
                        request: TextClassification.Request,
                    ): TextClassification {
                        classifiedSelectionCount++
                        return TextClassification.Builder().addAction(openLinkAction).build()
                    }

                    override fun generateLinks(request: TextLinks.Request): TextLinks {
                        val text = request.text.toString()
                        val builder = TextLinks.Builder(text)
                        LINK_PATTERNS.forEach { (entityType, pattern) ->
                            pattern.findAll(input = text).forEach { match ->
                                builder.addLink(
                                    match.range.first,
                                    match.range.last + 1,
                                    mapOf(entityType to 1f),
                                )
                            }
                        }
                        return builder.build()
                    }
                },
            )
    }

    @Test
    fun touchExploration_linkTapOfMessageWithoutTapNeitherOpensNorSelects() {
        enableTouchExploration()
        setLinkMessageContent()

        clickLinkText()
        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = ViewConfiguration.getLongPressTimeout() * 2L,
        )

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onExternalUriClick.invoke(any())
            }
            verify(exactly = 0) {
                onMessageLongClick.invoke()
            }
        }
    }

    @Test
    fun linkLongClick_firstTapDismissesAndSecondTapOpensTheLink() {
        setLinkMessageContent()

        composeTestRule
            .onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
            .performTouchInput { longClick(position = centerLeft) }
        clickLinkText()
        composeTestRule.runOnIdle {
            verify(exactly = 0) { onExternalUriClick.invoke(any()) }
        }
        clickLinkText(delayMillis = ViewConfiguration.getDoubleTapTimeout().toLong())

        composeTestRule.runOnIdle {
            verify(exactly = 0) {
                onMessageLongClick.invoke()
            }
            verify(exactly = 1) {
                onExternalUriClick.invoke(any())
            }
        }
    }

    @Test
    fun linkLongClick_dismissingTapLeavesTheClosingToolbarWithoutItems() {
        setLinkMessageContent()

        pressAndHoldLinkStart().performTouchInput { up() }
        assertTrue(textContextMenuToolbar.closedMenuItemLabels().isNotEmpty())
        clickLinkText()

        composeTestRule.runOnIdle {
            assertFalse(textContextMenuToolbar.isShown)
            assertEquals(emptyList<String>(), textContextMenuToolbar.closedMenuItemLabels())
        }
    }

    @Test
    fun linkLongPress_offersToCopyTheLinkBeforeTheFingerLifts() {
        setLinkMessageContent()

        val linkText = pressAndHoldLinkStart()

        assertNotNull(textContextMenuToolbar.item(label = copyLinkLabel()))
        linkText.performTouchInput { up() }
    }

    @Test
    fun selectedLink_opensThroughTheAppInsteadOfTheSystemAction() {
        setLinkMessageContent()

        selectTextAndAwaitToolbar(text = LINK_TEXT)
        assertEquals(0, textContextMenuToolbar.systemActionCount())
        textContextMenuToolbar.click(label = openLinkLabel())

        composeTestRule.runOnIdle {
            verify(exactly = 1) { onExternalUriClick(LINK_TEXT) }
        }
    }

    @Test
    fun selectedTextWithoutALink_keepsTheSystemActionWhenLinksOpenOnTap() {
        setConversationMessageContent(
            message = message(text = PLAIN_TEXT),
            textContextMenuToolbar = textContextMenuToolbar,
            linkOpeningMode = LinkOpeningMode.OpenOnTap,
        )

        selectTextAndAwaitToolbar(
            text = PLAIN_TEXT,
            awaitedItemLabel = selectMessageLabel(),
        )

        assertEquals(1, textContextMenuToolbar.systemActionCount())
    }

    @Test
    fun selectedSubjectLink_offersNoWayToOpenItWhenAskingBeforeOpening() {
        selectSubjectLink(linkOpeningMode = LinkOpeningMode.AskBeforeOpening)

        composeTestRule.runOnIdle {
            assertEquals(0, textContextMenuToolbar.systemActionCount())
            assertNull(textContextMenuToolbar.item(label = openLinkLabel()))
        }
    }

    @Test
    fun selectedSubjectLink_keepsTheSystemActionWhenLinksOpenOnTap() {
        selectSubjectLink(linkOpeningMode = LinkOpeningMode.OpenOnTap)

        assertEquals(1, textContextMenuToolbar.systemActionCount())
    }

    @Test
    fun selectedDisabledLink_offersNoSystemActionButStillCopiesTheWholeLink() {
        setConversationMessageContent(
            message = message(text = LINK_TEXT),
            textContextMenuToolbar = textContextMenuToolbar,
            linkOpeningMode = LinkOpeningMode.DoNotOpen,
        )
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            copyLinkActionLabels(text = LINK_TEXT).isNotEmpty()
        }

        selectTextAndAwaitToolbar(text = LINK_TEXT)
        assertEquals(0, textContextMenuToolbar.systemActionCount())
        assertNull(textContextMenuToolbar.item(label = openLinkLabel()))
        textContextMenuToolbar.click(label = copyLinkLabel())

        composeTestRule.runOnIdle {
            verify(exactly = 1) { onCopyText(LINK_TEXT) }
        }
    }

    @Test
    fun linkLongPressUnderASubject_copiesTheWholeLinkWithoutALeadingNewline() {
        setLinkMessageContentUnderASubject()

        val linkText = pressAndHoldLinkStart()
        textContextMenuToolbar.clickCopy()
        linkText.performTouchInput { up() }

        assertEquals(LINK_TEXT, copiedText())
    }

    @Test
    fun linkLongPressUnderASubject_copyLinkCopiesTheWholeLink() {
        setLinkMessageContentUnderASubject()

        val linkText = pressAndHoldLinkStart()
        assertNotNull(textContextMenuToolbar.item(label = copyLinkLabel()))
        linkText.performTouchInput { up() }
        // The release shows the toolbar again, after text classification off the main clock
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            textContextMenuToolbar.item(label = copyLinkLabel()) != null
        }
        textContextMenuToolbar.click(label = copyLinkLabel())

        composeTestRule.runOnIdle {
            verify(exactly = 1) { onCopyText(LINK_TEXT) }
        }
    }

    @Test
    fun messageOffersToCopyEachOfItsLinksWithoutSelectingThem() {
        enableTouchExploration()
        setConversationMessageContent(message = message(text = TWO_LINKS_TEXT))
        awaitLinkAnnotated(text = TWO_LINKS_TEXT)

        copyLinkTargetAction(text = TWO_LINKS_TEXT, target = LINK_TEXT).action()
        copyLinkTargetAction(text = TWO_LINKS_TEXT, target = OTHER_LINK_TEXT).action()

        composeTestRule.runOnIdle {
            verify(exactly = 1) { onCopyText(LINK_TEXT) }
            verify(exactly = 1) { onCopyText(OTHER_LINK_TEXT) }
        }
    }

    @Test
    fun phoneNumberAndEmailAddressActionsCopyTheTextAsWritten() {
        setConversationMessageContent(message = message(text = CONTACT_DETAILS_TEXT))
        awaitLinkAnnotated(text = CONTACT_DETAILS_TEXT)

        requireNotNull(
            findCustomAction(
                text = CONTACT_DETAILS_TEXT,
                label = targetContext.getString(
                    R.string.conversation_copy_phone_number_target,
                    PHONE_NUMBER_TEXT,
                ),
            ),
        ).action()
        requireNotNull(
            findCustomAction(
                text = CONTACT_DETAILS_TEXT,
                label = targetContext.getString(
                    R.string.conversation_copy_email_address_target,
                    EMAIL_ADDRESS_TEXT,
                ),
            ),
        ).action()

        composeTestRule.runOnIdle {
            verify(exactly = 1) { onCopyText(PHONE_NUMBER_TEXT) }
            verify(exactly = 1) { onCopyText(EMAIL_ADDRESS_TEXT) }
        }
    }

    @Test
    fun repeatedLinkIsOfferedOnce() {
        val text = "$LINK_TEXT $LINK_TEXT"
        setConversationMessageContent(message = message(text = text))
        awaitLinkAnnotated(text = text)

        assertEquals(
            listOf(targetContext.getString(R.string.conversation_copy_link_target, LINK_TEXT)),
            copyLinkActionLabels(text = text),
        )
    }

    @Test
    fun selectingTheLinkAddsNoSecondCopyLinkAction() {
        setLinkMessageContent()

        requireNotNull(
            findCustomAction(
                text = LINK_TEXT,
                label = targetContext.getString(R.string.conversation_message_select_text),
            ),
        ).action()

        assertEquals(
            listOf(targetContext.getString(R.string.conversation_copy_link_target, LINK_TEXT)),
            copyLinkActionLabels(text = LINK_TEXT),
        )
    }

    private fun setLinkMessageContent() {
        setConversationMessageContent(
            message = message(text = LINK_TEXT),
            textContextMenuToolbar = textContextMenuToolbar,
        )
        awaitLinkAnnotated(text = LINK_TEXT)
    }

    private fun setLinkMessageContentUnderASubject() {
        setConversationMessageContent(
            message = message(
                text = LINK_TEXT,
                mmsSubject = SUBJECT_TEXT,
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            textContextMenuToolbar = textContextMenuToolbar,
        )
        awaitLinkAnnotated(text = LINK_TEXT)
    }

    private fun selectSubjectLink(linkOpeningMode: LinkOpeningMode) {
        setConversationMessageContent(
            message = message(
                text = null,
                mmsSubject = LINK_TEXT,
                protocol = ConversationMessageUiModel.Protocol.MMS,
            ),
            textContextMenuToolbar = textContextMenuToolbar,
            linkOpeningMode = linkOpeningMode,
        )
        selectTextAndAwaitToolbar(text = LINK_TEXT, awaitedItemLabel = selectMessageLabel())
    }

    private fun openLinkAction(context: Context): RemoteAction {
        return RemoteAction(
            Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)),
            OPEN_LINK_ACTION_TITLE,
            OPEN_LINK_ACTION_TITLE,
            PendingIntent.getActivity(
                context,
                0,
                Intent(Intent.ACTION_VIEW, LINK_TEXT.toUri()),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    private fun copiedText(): String {
        val clipboard = targetContext.getSystemService(ClipboardManager::class.java)
        return composeTestRule.runOnIdle {
            clipboard.primaryClip?.getItemAt(0)?.text.toString()
        }
    }

    private fun pressAndHoldLinkStart(): SemanticsNodeInteraction {
        val linkText = composeTestRule.onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
        linkText.performTouchInput { down(position = centerLeft) }
        composeTestRule.mainClock.advanceTimeBy(
            milliseconds = ViewConfiguration.getLongPressTimeout() * 2L,
        )
        // The toolbar shows only after text classification, which runs off the main clock.
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            textContextMenuToolbar.isShown
        }
        return linkText
    }

    // A long press outside a link shows the platform magnifier, which Robolectric cannot dismiss.
    private fun selectTextAndAwaitToolbar(
        text: String,
        awaitedItemLabel: String = copyLinkLabel(),
    ) {
        requireNotNull(
            findCustomAction(
                text = text,
                label = targetContext.getString(R.string.conversation_message_select_text),
            ),
        ).action()
        // The toolbar shows only after text classification, which runs off the main clock.
        composeTestRule.waitUntil(timeoutMillis = TEST_WAIT_TIMEOUT_MILLIS) {
            classifiedSelectionCount > 0 && textContextMenuToolbar.item(awaitedItemLabel) != null
        }
    }

    private fun clickLinkText(delayMillis: Long = 0) {
        composeTestRule
            .onNodeWithText(text = LINK_TEXT, useUnmergedTree = true)
            .performTouchInput {
                advanceEventTime(durationMillis = delayMillis)
                click(position = centerLeft)
            }
    }

    private fun copyLinkLabel(): String {
        return targetContext.getString(R.string.conversation_copy_link)
    }

    private fun openLinkLabel(): String {
        return targetContext.getString(R.string.conversation_open_link)
    }

    private fun selectMessageLabel(): String {
        return targetContext.getString(R.string.conversation_message_select_message)
    }

    private fun copyLinkTargetAction(text: String, target: String): CustomAccessibilityAction {
        val label = targetContext.getString(R.string.conversation_copy_link_target, target)
        return requireNotNull(findCustomAction(text = text, label = label))
    }

    private fun findCustomAction(text: String, label: String): CustomAccessibilityAction? {
        return customActions(text = text).singleOrNull { it.label == label }
    }

    private fun copyLinkActionLabels(text: String): List<String> {
        return customActions(text = text)
            .map { it.label }
            .filter { it.startsWith(prefix = copyLinkLabel()) }
    }

    private fun customActions(text: String): List<CustomAccessibilityAction> {
        return composeTestRule
            .onNodeWithText(text = text)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
    }

    private class RecordingTextContextMenuToolbar : TextContextMenuProvider {
        private var shownMenu: TextContextMenuDataProvider? = null
        private var lastShownMenu: TextContextMenuDataProvider? = null

        val isShown: Boolean
            get() = shownMenu != null

        override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
            shownMenu = dataProvider
            lastShownMenu = dataProvider
            try {
                awaitCancellation()
            } finally {
                shownMenu = null
            }
        }

        fun item(label: String): TextContextMenuItem? {
            return items().singleOrNull { it.label == label }
        }

        fun click(label: String) {
            click(item = requireNotNull(item(label = label)))
        }

        fun clickCopy() {
            click(item = items().single { it.key == TextContextMenuKeys.CopyKey })
        }

        // Compose adds the classifier's actions as its own internal component type.
        fun systemActionCount(): Int {
            return shownMenu
                ?.data()
                ?.components
                ?.count { it !is TextContextMenuItem && it !== TextContextMenuSeparator }
                ?: 0
        }

        // The platform toolbar keeps redrawing from its data while its dismiss animation runs.
        fun closedMenuItemLabels(): List<String> {
            return items(menu = lastShownMenu).map { it.label }
        }

        private fun items(
            menu: TextContextMenuDataProvider? = shownMenu,
        ): List<TextContextMenuItem> {
            return menu
                ?.data()
                ?.components
                ?.filterIsInstance<TextContextMenuItem>()
                .orEmpty()
        }

        private fun click(item: TextContextMenuItem) {
            item.onClick(
                object : TextContextMenuSession {
                    override fun close() {}
                },
            )
        }
    }

    private companion object {
        private const val LINK_TEXT = "https://example.com"
        private const val OTHER_LINK_TEXT = "https://example.org"
        private const val TWO_LINKS_TEXT = "Read $LINK_TEXT or $OTHER_LINK_TEXT tonight"
        private const val PHONE_NUMBER_TEXT = "+1-555-0100"
        private const val EMAIL_ADDRESS_TEXT = "name@example.com"
        private const val CONTACT_DETAILS_TEXT = "Call $PHONE_NUMBER_TEXT or $EMAIL_ADDRESS_TEXT"
        private const val SUBJECT_TEXT = "Message subject"
        private const val PLAIN_TEXT = "See you tonight"
        private const val OPEN_LINK_ACTION_TITLE = "Open"
        private val LINK_PATTERNS = mapOf(
            TextClassifier.TYPE_URL to Regex(pattern = "https://\\S+"),
            TextClassifier.TYPE_PHONE to Regex(pattern = "\\+[\\d-]+"),
            TextClassifier.TYPE_EMAIL to Regex(pattern = "\\w+@\\S+"),
        )
    }
}
