package com.android.messaging.screenshots

import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.conversation.model.ParticipantId
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.ui.contact.model.ContactDestinationUiModel
import com.android.messaging.ui.contact.model.ContactUiModel
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel.Attachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Protocol
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel.Status
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import com.android.messaging.ui.conversation.preview.previewComposerUiState
import com.android.messaging.ui.conversation.preview.previewGroupMetadata
import com.android.messaging.ui.conversation.preview.previewIncomingMessage
import com.android.messaging.ui.conversation.preview.previewMetadata
import com.android.messaging.ui.conversation.preview.previewOutgoingMessage
import com.android.messaging.ui.conversation.preview.previewSubscription
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.conversationlist.chats.model.ConversationListUiState
import com.android.messaging.ui.conversationlist.common.support.previewConversationListItem
import com.android.messaging.ui.conversationlist.model.ConversationListContentUiState
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListPreviewUiModel
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private const val MINUTES_PER_DAY = 24 * 60L

// Every name and number is invented; 555-01xx is reserved for fiction.
private class Person(
    val name: String,
    val destination: String,
) {
    val participantId = ParticipantId(destination)
}

private val Sam = Person(name = "Sam Rivera", destination = "+15550134")
private val Maya = Person(name = "Maya Chen", destination = "+15550187")
private val Jonas = Person(name = "Jonas Weber", destination = "+15550112")
private val Priya = Person(name = "Priya Nair", destination = "+15550165")

// The list formats times against the real clock ("12 mins", "Mon"), so the screenshots run in a
// zone whose clock reads this time now, and every screen places its times around it.
internal val ReadmeSceneTime: LocalTime = LocalTime.of(18, 32)

internal val ReadmeSceneZone: ZoneOffset = sceneZone()

internal fun readmeConversationListUiState(): ConversationListUiState {
    val items = persistentListOf(
        listItem(
            title = "Cabin weekend",
            destination = null,
            timestamp = minutesAgo(minutes = 2),
            snippet = "The host, just in case",
            senderName = "Priya",
            isUnread = true,
            isPinned = true,
        ),
        listItem(
            title = Sam.name,
            destination = Sam.destination,
            timestamp = at(daysAgo = 0, hour = 10, minute = 9),
            snippet = "Train's on me 🚆",
            isOutgoing = true,
            isPinned = true,
        ),
        listItem(
            title = "72633",
            destination = "72633",
            timestamp = minutesAgo(minutes = 12),
            snippet = "Your code is 482913. Don't share it.",
            isUnread = true,
        ),
        listItem(
            title = Priya.name,
            destination = Priya.destination,
            timestamp = minutesAgo(minutes = 47),
            snippet = "The view from the top 🏔️",
            preview = ConversationListPreviewUiModel.Image(
                "readme://hike_meadow.jpg",
                "image/jpeg",
            ),
        ),
        listItem(
            title = "+1 555-0199",
            destination = "+15550199",
            timestamp = at(daysAgo = 0, hour = 16, minute = 12),
            snippet = "Hi! Is the bike still for sale?",
            isUnread = true,
        ),
        listItem(
            title = Jonas.name,
            destination = Jonas.destination,
            timestamp = at(daysAgo = 0, hour = 14, minute = 50),
            snippet = "Sure, I'll bring the",
            isDraft = true,
        ),
        listItem(
            title = "Book club",
            destination = null,
            timestamp = at(daysAgo = 1, hour = 16, minute = 5),
            snippet = "Chapter 12 for Thursday 📚",
            senderName = "Lena",
            isMuted = true,
        ),
        listItem(
            title = "Alex Kim",
            destination = "+15550121",
            timestamp = at(daysAgo = 3, hour = 19, minute = 40),
            snippet = "See you at 8!",
        ),
        listItem(
            title = "Climbing crew",
            destination = null,
            timestamp = at(daysAgo = 5, hour = 21, minute = 3),
            snippet = "Wednesday 7pm at the wall?",
            senderName = "Tom",
        ),
        listItem(
            title = "Riverside Dental",
            destination = "+15550148",
            timestamp = at(daysAgo = 8, hour = 9, minute = 15),
            snippet = "Reminder: your appointment is Thu at 9:30",
        ),
    )
    return ConversationListUiState(content = ConversationListContentUiState.Items(items))
}

internal fun readmeRecipientPickerUiState(): RecipientPickerUiState {
    return RecipientPickerUiState(
        query = "Ma",
        items = persistentListOf(
            contact(id = 1, name = Maya.name, number = "+1 555-0187", secondNumber = "+1 555-0190"),
            contact(id = 2, name = "Marco Bianchi", number = "+1 555-0153"),
            contact(id = 3, name = "Mateo García", number = "+1 555-0138"),
            contact(id = 4, name = "Emma Martin", number = "+1 555-0129"),
            contact(id = 5, name = "Malik Johnson", number = "+1 555-0171"),
            contact(id = 6, name = "Mark Thompson", number = "+1 555-0115"),
            contact(id = 7, name = "Martina López", number = "+1 555-0196"),
        ),
    )
}

internal fun readmeChatUiState(): ConversationScreenScaffoldUiState {
    return ConversationScreenScaffoldUiState(
        canCall = true,
        metadata = previewMetadata(title = Sam.name).copy(
            avatar = ConversationMetadataUiState.Avatar.Single(
                photoUri = null,
                normalizedDestination = Sam.destination,
                displayName = Sam.name,
            ),
        ),
        messages = ConversationMessagesUiState.Present(
            messages = persistentListOf(
                outgoing(at(daysAgo = 1, 21, 16), "Best one on YouTube 👇 youtu.be/dQw4w9WgXcQ"),
                incoming(Sam, at(daysAgo = 1, 21, 17), "I can't believe I fell for that 😂"),
                incoming(
                    sender = Sam,
                    at = at(daysAgo = 0, 10, 2),
                    text = "Lago di Braies. Going back in October, come along?",
                    attachments = listOf(photo("hike_lake.jpg"), photo("hike_meadow.jpg")),
                ),
                outgoing(at(daysAgo = 0, 10, 8), "I'm in 😍"),
                outgoing(
                    at = at(daysAgo = 0, 10, 9),
                    text = "Train's on me 🚆",
                    status = Status.Outgoing.Delivered,
                ),
            ),
        ),
        composer = readmeComposer(),
    )
}

internal fun readmeGroupChatUiState(): ConversationScreenScaffoldUiState {
    return ConversationScreenScaffoldUiState(
        metadata = previewGroupMetadata().copy(title = "Cabin weekend"),
        messages = ConversationMessagesUiState.Present(
            messages = persistentListOf(
                incoming(
                    sender = Maya,
                    at = at(daysAgo = 0, 18, 21),
                    text = "Booked it! Our cabin 🎉",
                    attachments = listOf(photo("cabin.jpg")),
                ),
                incoming(
                    sender = Jonas,
                    at = at(daysAgo = 0, 18, 24),
                    text = null,
                    attachments = listOf(voiceNote(durationMillis = 14_000L)),
                ),
                incoming(
                    sender = Priya,
                    at = at(daysAgo = 0, 18, 30),
                    text = "The host, just in case",
                    attachments = listOf(
                        contactCard(name = "Erik Lindqvist", number = "+1 555-0143")
                    ),
                ),
                outgoing(
                    at(daysAgo = 0, 18, 33),
                    "I'm on pancake duty 🥞",
                    status = Status.Outgoing.Delivered
                ),
            ),
        ),
        composer = readmeComposer(),
    )
}

private fun listItem(
    title: String,
    destination: String?,
    timestamp: Long,
    snippet: String,
    senderName: String? = null,
    preview: ConversationListPreviewUiModel? = null,
    isOutgoing: Boolean = false,
    isUnread: Boolean = false,
    isPinned: Boolean = false,
    isMuted: Boolean = false,
    isDraft: Boolean = false,
): ConversationListItemUiModel {
    val item = previewConversationListItem(
        conversationId = ConversationId(title),
        title = title,
        snippetText = snippet,
        senderName = senderName,
        status = when {
            isDraft -> ConversationListMessageStatus.Draft
            else -> ConversationListMessageStatus.Normal
        },
        preview = preview,
        isOutgoing = isOutgoing,
        isUnread = isUnread,
        isGroup = destination == null,
        isMuted = isMuted,
        isPinned = isPinned,
        isDraft = isDraft,
    )
    return item.copy(
        avatar = item.avatar.copy(normalizedDestination = destination, subtitle = destination),
        timestampMillis = timestamp,
    )
}

private fun contact(
    id: Long,
    name: String,
    number: String,
    secondNumber: String? = null,
): RecipientPickerListItem.Contact {
    // 2 and 3 are ContactsContract's mobile and work phone types.
    val numbers = listOfNotNull(number to 2, secondNumber?.let { it to 3 })
    return RecipientPickerListItem.Contact(
        ContactUiModel(
            id = id,
            lookupKey = "readme-$id",
            displayName = name,
            photoUri = null,
            destinations = numbers.mapIndexed { index, (value, type) ->
                ContactDestinationUiModel(
                    dataId = id * 10 + index,
                    contactId = id,
                    value = value,
                    normalizedValue = value.filterNot { it == ' ' || it == '-' },
                    displayValue = value,
                    kind = ContactDestinationUiModel.Kind.PHONE,
                    type = type,
                    customLabel = null,
                    isPrimary = index == 0,
                    isSuperPrimary = index == 0,
                )
            }.toPersistentList(),
        ),
    )
}

private fun readmeComposer(): ConversationComposerUiState {
    val subscription = previewSubscription()
    return previewComposerUiState(messageText = "").copy(
        segmentCounter = null,
        simSelector = ConversationSimSelectorUiState(
            subscriptions = persistentListOf(subscription),
            selectedSubscription = subscription,
        ),
    )
}

private fun incoming(
    sender: Person,
    at: Long,
    text: String?,
    attachments: List<Attachment> = emptyList(),
): ConversationMessageUiModel {
    val parts = attachments + listOfNotNull(text?.let(ConversationMessagePartUiModel::Text))
    return previewIncomingMessage(
        messageId = MessageId("${sender.destination}-$at"),
        text = text,
        parts = persistentListOf(*parts.toTypedArray()),
        protocol = if (attachments.isEmpty()) Protocol.SMS else Protocol.MMS,
    ).copy(
        senderDisplayName = sender.name,
        senderParticipantId = sender.participantId,
        senderNormalizedDestination = sender.destination,
        senderContactLookupKey = null,
        sentTimestamp = at,
        receivedTimestamp = at,
        displayTimestamp = at,
    )
}

private fun outgoing(
    at: Long,
    text: String,
    status: Status = Status.Outgoing.Complete,
): ConversationMessageUiModel {
    return previewOutgoingMessage(
        messageId = MessageId("self-$at"),
        text = text,
        status = status,
    ).copy(
        sentTimestamp = at,
        receivedTimestamp = at,
        displayTimestamp = at,
    )
}

private fun photo(name: String): Attachment.Image {
    return Attachment.Image(
        text = null,
        contentType = "image/jpeg",
        contentUri = "readme://$name".toUri(),
        width = 1080,
        height = 720,
        partId = name,
    )
}

private fun voiceNote(durationMillis: Long): Attachment.Audio {
    return Attachment.Audio(
        text = null,
        contentType = "audio/ogg",
        contentUri = "readme://voice.ogg".toUri(),
        width = 0,
        height = 0,
        partId = "voice",
        durationMillis = durationMillis,
    )
}

private fun contactCard(name: String, number: String): Attachment.VCard {
    return Attachment.VCard(
        text = null,
        contentType = "text/vcard",
        contentUri = "readme://contact.vcf".toUri(),
        width = 0,
        height = 0,
        vCardUiModel = ConversationVCardAttachmentUiModel(
            type = ConversationVCardAttachmentType.CONTACT,
            normalizedDestination = number.filterNot { it == ' ' || it == '-' },
            titleText = name,
            subtitleText = number,
        ),
        partId = "contact",
    )
}

private fun at(daysAgo: Long, hour: Int, minute: Int): Long {
    return LocalDate.now(ReadmeSceneZone)
        .minusDays(daysAgo)
        .atTime(hour, minute)
        .atZone(ReadmeSceneZone)
        .toInstant()
        .toEpochMilli()
}

// For times the list shows as "12 mins", which counts from the real clock, not the scene's minute.
private fun minutesAgo(minutes: Long): Long {
    return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes)
}

private fun sceneZone(): ZoneOffset {
    val utcNow = LocalTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES)
    val minutesAhead = Duration.between(utcNow, ReadmeSceneTime).toMinutes()
    val offsetMinutes = Math.floorMod(minutesAhead + MINUTES_PER_DAY / 2, MINUTES_PER_DAY) -
        MINUTES_PER_DAY / 2
    return ZoneOffset.ofTotalSeconds(Math.toIntExact(TimeUnit.MINUTES.toSeconds(offsetMinutes)))
}
