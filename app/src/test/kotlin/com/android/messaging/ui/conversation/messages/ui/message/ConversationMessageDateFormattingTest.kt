package com.android.messaging.ui.conversation.messages.ui.message

import android.content.Context
import android.text.format.DateUtils
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class ConversationMessageDateFormattingTest {

    @After
    fun tearDown() {
        unmockkStatic(DateUtils::class)
    }

    @Test
    fun displayEpochDay_returnsNullForNonPositiveTimestamp() {
        assertNull(
            conversationMessageDisplayEpochDay(
                displayTimestamp = 0L,
                timeZone = TimeZone.getTimeZone("UTC"),
            ),
        )
        assertNull(
            conversationMessageDisplayEpochDay(
                displayTimestamp = -1L,
                timeZone = TimeZone.getTimeZone("UTC"),
            ),
        )
    }

    @Test
    fun displayEpochDay_usesProvidedTimezoneOffset() {
        val timestamp = 64_800_000L

        assertEquals(
            0L,
            conversationMessageDisplayEpochDay(
                displayTimestamp = timestamp,
                timeZone = TimeZone.getTimeZone("UTC"),
            ),
        )
        assertEquals(
            1L,
            conversationMessageDisplayEpochDay(
                displayTimestamp = timestamp,
                timeZone = TimeZone.getTimeZone("GMT+12"),
            ),
        )
    }

    @Test
    fun formatDateSeparatorText_returnsNullForNonPositiveTimestamp() {
        assertNull(
            formatDateSeparatorText(
                context = mockk(),
                message = message(displayTimestamp = 0L),
            ),
        )
    }

    @Test
    fun formatDateSeparatorText_omitsYearForCurrentYearTimestamp() {
        mockkStatic(DateUtils::class)
        val context = mockk<Context>()
        val timestamp = timestampForDate(date = LocalDate.now())
        val expectedFlags = DateUtils.FORMAT_SHOW_WEEKDAY or
            DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_NO_YEAR
        every {
            DateUtils.formatDateTime(
                context,
                timestamp,
                expectedFlags,
            )
        } returns "Today"

        assertEquals(
            "Today",
            formatDateSeparatorText(
                context = context,
                message = message(displayTimestamp = timestamp),
            ),
        )
    }

    @Test
    fun formatDateSeparatorText_includesYearForDifferentYearTimestamp() {
        mockkStatic(DateUtils::class)
        val context = mockk<Context>()
        val timestamp = timestampForDate(date = LocalDate.now().minusYears(1))
        val expectedFlags = DateUtils.FORMAT_SHOW_WEEKDAY or
            DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_SHOW_YEAR
        every {
            DateUtils.formatDateTime(
                context,
                timestamp,
                expectedFlags,
            )
        } returns "Last year"

        assertEquals(
            "Last year",
            formatDateSeparatorText(
                context = context,
                message = message(displayTimestamp = timestamp),
            ),
        )
    }

    private fun timestampForDate(date: LocalDate): Long {
        return date
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun message(displayTimestamp: Long): ConversationMessageUiModel {
        return ConversationMessageUiModel(
            messageId = "message-1",
            conversationId = "conversation-1",
            text = "Hello",
            parts = persistentListOf(),
            sentTimestamp = displayTimestamp,
            receivedTimestamp = displayTimestamp,
            displayTimestamp = displayTimestamp,
            status = ConversationMessageUiModel.Status.Outgoing.Complete,
            isIncoming = false,
            senderDisplayName = null,
            senderAvatarUri = null,
            senderContactId = 0L,
            senderContactLookupKey = null,
            senderNormalizedDestination = null,
            senderParticipantId = null,
            selfParticipantId = null,
            canClusterWithPrevious = false,
            canClusterWithNext = false,
            canCopyMessageToClipboard = true,
            canDownloadMessage = false,
            canForwardMessage = true,
            canResendMessage = false,
            canSaveAttachments = false,
            mmsDownload = null,
            mmsSubject = null,
            protocol = ConversationMessageUiModel.Protocol.SMS,
        )
    }
}
