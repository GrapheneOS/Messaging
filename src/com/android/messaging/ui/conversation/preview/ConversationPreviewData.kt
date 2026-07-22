@file:Suppress("TooManyFunctions")

package com.android.messaging.ui.conversation.preview

import androidx.core.net.toUri
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.conversation.model.metadata.ConversationComposerAvailability
import com.android.messaging.data.conversation.model.metadata.ConversationSubscriptionLabel
import com.android.messaging.data.subscription.model.Subscription
import com.android.messaging.data.vcard.model.VCardAvatarPhoto
import com.android.messaging.domain.conversation.usecase.draft.model.ConversationDraftSendProtocol
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingPhase
import com.android.messaging.ui.conversation.audio.model.ConversationAudioRecordingUiState
import com.android.messaging.ui.conversation.composer.model.ComposerAttachmentUiModel
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSegmentCounterUiState
import com.android.messaging.ui.conversation.composer.model.ConversationSimSelectorUiState
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentItem
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentOpenAction
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationAttachmentSections
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationInlineAttachment
import com.android.messaging.ui.conversation.messages.model.attachment.ConversationMessageAttachment
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagePartUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessageUiModel
import com.android.messaging.ui.conversation.messages.model.message.ConversationMessagesUiState
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import com.android.messaging.ui.conversation.metadata.model.ConversationMetadataUiState
import java.util.Base64
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val PREVIEW_NOW_MILLIS = 1_806_240_000_000L
private const val PREVIEW_MESSAGE_RECEIVED_MILLIS = PREVIEW_NOW_MILLIS - 120_000L
private const val PREVIEW_IMAGE_WIDTH = 1600
private const val PREVIEW_IMAGE_HEIGHT = 1200
private const val PREVIEW_AVATAR_PNG_BASE64 =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADUlEQVR42mP8z8BQDwAFgwJ/l0SWJAAAAABJRU5ErkJggg=="

internal fun previewSubscriptions(): ImmutableList<Subscription> {
    return persistentListOf(
        previewSubscription(
            selfParticipantId = "self-1",
            subId = 1,
            label = ConversationSubscriptionLabel.Named(name = "Personal"),
            displayDestination = "+31 6 1234 5678",
            displaySlotId = 1,
            color = 0xff1e88e5.toInt(),
        ),
        previewSubscription(
            selfParticipantId = "self-2",
            subId = 2,
            label = ConversationSubscriptionLabel.Named(name = "Work"),
            displayDestination = "+372 5555 0101",
            displaySlotId = 2,
            color = 0xff43a047.toInt(),
        ),
    )
}

internal fun previewSubscription(
    selfParticipantId: String = "self-1",
    subId: Int = 1,
    label: ConversationSubscriptionLabel = ConversationSubscriptionLabel.Named(name = "Personal"),
    displayDestination: String? = "+31 6 1234 5678",
    displaySlotId: Int = 1,
    color: Int = 0xff1e88e5.toInt(),
): Subscription {
    return Subscription(
        selfParticipantId = selfParticipantId,
        subId = subId,
        label = label,
        displayDestination = displayDestination,
        displaySlotId = displaySlotId,
        color = color,
    )
}

internal fun previewSimSelectorUiState(): ConversationSimSelectorUiState {
    val subscriptions = previewSubscriptions()
    return ConversationSimSelectorUiState(
        subscriptions = subscriptions,
        selectedSubscription = subscriptions.first(),
        isLoading = false,
    )
}

internal fun previewMetadata(
    title: String = "Ada Lovelace",
    participantCount: Int = 1,
    isBlocked: Boolean = false,
): ConversationMetadataUiState.Present {
    return ConversationMetadataUiState.Present(
        title = title,
        selfParticipantId = "self-1",
        avatar = ConversationMetadataUiState.Avatar.Single(
            photoUri = null,
            normalizedDestination = null,
            displayName = null,
        ),
        participantCount = participantCount,
        otherParticipantDisplayDestination = "+31 6 2222 3333",
        otherParticipantPhoneNumber = "+31622223333",
        otherParticipantContactLookupKey = "preview-contact",
        isArchived = false,
        isBlocked = isBlocked,
        composerAvailability = ConversationComposerAvailability.Editable,
    )
}

internal fun previewGroupMetadata(): ConversationMetadataUiState.Present {
    return ConversationMetadataUiState.Present(
        title = "Project group",
        selfParticipantId = "self-1",
        avatar = ConversationMetadataUiState.Avatar.Group,
        participantCount = 4,
        otherParticipantDisplayDestination = null,
        otherParticipantPhoneNumber = null,
        otherParticipantContactLookupKey = null,
        isArchived = false,
        isBlocked = false,
        composerAvailability = ConversationComposerAvailability.Editable,
    )
}

internal fun previewComposerUiState(
    messageText: String = "Sounds good, see you at 18:30.",
    subjectText: String = "",
): ConversationComposerUiState {
    return ConversationComposerUiState(
        attachments = persistentListOf(),
        messageText = messageText,
        subjectText = subjectText,
        selfParticipantId = "self-1",
        simSelector = previewSimSelectorUiState(),
        isMessageFieldEnabled = true,
        isAttachmentActionEnabled = true,
        isRecordActionEnabled = true,
        isSendEnabled = messageText.isNotBlank() || subjectText.isNotBlank(),
        shouldShowRecordAction = messageText.isBlank() && subjectText.isBlank(),
        hasWorkingDraft = messageText.isNotBlank() || subjectText.isNotBlank(),
        sendProtocol = ConversationDraftSendProtocol.SMS,
        attachmentCount = 0,
        pendingAttachmentCount = 0,
        segmentCounter = ConversationSegmentCounterUiState(
            codePointsRemainingInCurrentMessage = 78,
            messageCount = 1,
        ),
    )
}

internal fun previewComposerWithAttachments(): ConversationComposerUiState {
    return previewComposerUiState(
        messageText = "Sharing the files from today.",
        subjectText = "Meeting notes",
    ).copy(
        attachments = persistentListOf(
            previewPendingAttachment(),
            previewResolvedImageAttachment(),
            previewResolvedAudioAttachment(),
            previewResolvedVCardAttachment(),
        ),
        sendProtocol = ConversationDraftSendProtocol.MMS,
        attachmentCount = 4,
        pendingAttachmentCount = 1,
        segmentCounter = null,
    )
}

internal fun previewRecordingComposer(
    isLocked: Boolean = false,
): ConversationComposerUiState {
    return previewComposerUiState(messageText = "").copy(
        audioRecording = ConversationAudioRecordingUiState(
            phase = ConversationAudioRecordingPhase.Recording,
            durationMillis = 41_000L,
            isLocked = isLocked,
        ),
        isSendEnabled = false,
        shouldShowRecordAction = true,
    )
}

internal fun previewPendingAttachment(): ComposerAttachmentUiModel.Pending.Generic {
    return ComposerAttachmentUiModel.Pending.Generic(
        key = "pending-file",
        contentType = "application/pdf",
        contentUri = "content://com.android.messaging.preview/pending/file.pdf",
        displayName = "Meeting agenda.pdf",
    )
}

internal fun previewPendingAudioAttachment(): ComposerAttachmentUiModel.Pending.AudioFinalizing {
    return ComposerAttachmentUiModel.Pending.AudioFinalizing(
        key = "pending-audio",
        contentType = "audio/ogg",
        contentUri = "content://com.android.messaging.preview/pending/audio.ogg",
        displayName = "Recording.ogg",
    )
}

internal fun previewResolvedImageAttachment():
    ComposerAttachmentUiModel.Resolved.VisualMedia.Image {
    return ComposerAttachmentUiModel.Resolved.VisualMedia.Image(
        key = "image-attachment",
        contentType = "image/jpeg",
        contentUri = "content://com.android.messaging.preview/image.jpg",
        captionText = "Photo from the event",
        width = PREVIEW_IMAGE_WIDTH,
        height = PREVIEW_IMAGE_HEIGHT,
    )
}

internal fun previewResolvedVideoAttachment():
    ComposerAttachmentUiModel.Resolved.VisualMedia.Video {
    return ComposerAttachmentUiModel.Resolved.VisualMedia.Video(
        key = "video-attachment",
        contentType = "video/mp4",
        contentUri = "content://com.android.messaging.preview/video.mp4",
        captionText = "Short clip",
        width = 1920,
        height = 1080,
    )
}

internal fun previewResolvedAudioAttachment(): ComposerAttachmentUiModel.Resolved.Audio {
    return ComposerAttachmentUiModel.Resolved.Audio(
        key = "audio-attachment",
        contentType = "audio/ogg",
        contentUri = "content://com.android.messaging.preview/audio.ogg",
        durationMillis = 72_000L,
    )
}

internal fun previewResolvedFileAttachment(): ComposerAttachmentUiModel.Resolved.File {
    return ComposerAttachmentUiModel.Resolved.File(
        key = "file-attachment",
        contentType = "application/pdf",
        contentUri = "content://com.android.messaging.preview/document.pdf",
    )
}

internal fun previewResolvedVCardAttachment(): ComposerAttachmentUiModel.Resolved.VCard {
    return ComposerAttachmentUiModel.Resolved.VCard(
        key = "vcard-attachment",
        contentType = "text/vcard",
        contentUri = "content://com.android.messaging.preview/contact.vcf",
        vCardUiModel = previewVCardUiModel(),
    )
}

internal fun previewVCardUiModel(
    type: ConversationVCardAttachmentType = ConversationVCardAttachmentType.CONTACT,
): ConversationVCardAttachmentUiModel {
    return ConversationVCardAttachmentUiModel(
        type = type,
        avatarPhoto = when (type) {
            ConversationVCardAttachmentType.CONTACT -> previewVCardAvatarPhoto()
            ConversationVCardAttachmentType.LOCATION -> null
        },
        normalizedDestination = when (type) {
            ConversationVCardAttachmentType.CONTACT -> "+31622223333"
            ConversationVCardAttachmentType.LOCATION -> null
        },
        titleText = when (type) {
            ConversationVCardAttachmentType.CONTACT -> "Ada Lovelace"
            ConversationVCardAttachmentType.LOCATION -> "Rathausmarkt"
        },
        subtitleText = when (type) {
            ConversationVCardAttachmentType.CONTACT -> "+31 6 2222 3333"
            ConversationVCardAttachmentType.LOCATION -> "Hamburg, Germany"
        },
    )
}

private fun previewVCardAvatarPhoto(): VCardAvatarPhoto {
    return VCardAvatarPhoto(Base64.getDecoder().decode(PREVIEW_AVATAR_PNG_BASE64))
}

internal fun previewMessagesUiState(): ConversationMessagesUiState.Present {
    return ConversationMessagesUiState.Present(messages = previewMessages())
}

internal fun previewMessages(): ImmutableList<ConversationMessageUiModel> {
    return persistentListOf(
        previewIncomingMessage(
            messageId = "incoming-mms",
            text = "Here are the photos and voice note.",
            status = ConversationMessageUiModel.Status.Incoming.Complete,
            parts = persistentListOf(
                previewImagePart(text = null),
                previewAudioPart(text = "Voice note"),
            ),
            canSaveAttachments = true,
        ),
        previewOutgoingMessage(
            messageId = "outgoing-delivered",
            text = "Received. I will forward them to the group.",
            status = ConversationMessageUiModel.Status.Outgoing.Delivered,
        ),
        previewIncomingMessage(
            messageId = "incoming-download",
            text = null,
            status = ConversationMessageUiModel.Status.Incoming.YetToManualDownload,
            mmsDownload = previewMmsDownloadUiModel(),
            protocol = ConversationMessageUiModel.Protocol.MMS_PUSH_NOTIFICATION,
            canDownloadMessage = true,
        ),
    )
}

internal fun previewIncomingMessage(
    messageId: String = "incoming-1",
    text: String? = "Can you review this before tonight?",
    status: ConversationMessageUiModel.Status = ConversationMessageUiModel.Status.Incoming.Complete,
    parts: ImmutableList<ConversationMessagePartUiModel> = persistentListOf(
        ConversationMessagePartUiModel.Text(text = text ?: "Preview message"),
    ),
    mmsDownload: MmsDownloadUiModel? = null,
    protocol: ConversationMessageUiModel.Protocol = ConversationMessageUiModel.Protocol.SMS,
    canDownloadMessage: Boolean = false,
    canSaveAttachments: Boolean = false,
): ConversationMessageUiModel {
    return previewMessage(
        messageId = messageId,
        text = text,
        parts = parts,
        status = status,
        isIncoming = true,
        senderDisplayName = "Ada Lovelace",
        senderParticipantId = "participant-ada",
        mmsDownload = mmsDownload,
        protocol = protocol,
        canDownloadMessage = canDownloadMessage,
        canSaveAttachments = canSaveAttachments,
    )
}

internal fun previewOutgoingMessage(
    messageId: String = "outgoing-1",
    text: String? = "I am on my way.",
    status: ConversationMessageUiModel.Status = ConversationMessageUiModel.Status.Outgoing.Complete,
    parts: ImmutableList<ConversationMessagePartUiModel> = persistentListOf(
        ConversationMessagePartUiModel.Text(text = text ?: "Preview reply"),
    ),
): ConversationMessageUiModel {
    return previewMessage(
        messageId = messageId,
        text = text,
        parts = parts,
        status = status,
        isIncoming = false,
        senderDisplayName = null,
        senderParticipantId = "self-1",
        selfParticipantId = "self-1",
    )
}

internal fun previewMessageAttachments(): ImmutableList<ConversationMessageAttachment> {
    return persistentListOf(
        ConversationMessageAttachment.Media(
            key = "message-image",
            part = previewImagePart(text = "Image caption"),
        ),
        ConversationMessageAttachment.Media(
            key = "message-video",
            part = previewVideoPart(text = "Video caption"),
        ),
        ConversationMessageAttachment.Unsupported(
            key = "message-file",
            part = previewFilePart(text = "Unsupported file"),
        ),
    )
}

internal fun previewAttachmentSections(): ConversationAttachmentSections {
    val attachments = previewMessageAttachments()
    return ConversationAttachmentSections(
        galleryVisualAttachments = persistentListOf(attachments[0], attachments[1]),
        trailingItems = persistentListOf(
            ConversationAttachmentItem.Inline(
                key = "inline-audio",
                attachment = previewInlineAudioAttachment(),
            ),
            ConversationAttachmentItem.Inline(
                key = "inline-vcard",
                attachment = previewInlineVCardAttachment(),
            ),
        ),
    )
}

internal fun previewImagePart(text: String?): ConversationMessagePartUiModel.Attachment.Image {
    return ConversationMessagePartUiModel.Attachment.Image(
        text = text,
        contentType = "image/jpeg",
        contentUri = "content://com.android.messaging.preview/message/image.jpg".toUri(),
        width = PREVIEW_IMAGE_WIDTH,
        height = PREVIEW_IMAGE_HEIGHT,
    )
}

internal fun previewVideoPart(text: String?): ConversationMessagePartUiModel.Attachment.Video {
    return ConversationMessagePartUiModel.Attachment.Video(
        text = text,
        contentType = "video/mp4",
        contentUri = "content://com.android.messaging.preview/message/video.mp4".toUri(),
        width = 1920,
        height = 1080,
    )
}

internal fun previewAudioPart(text: String?): ConversationMessagePartUiModel.Attachment.Audio {
    return ConversationMessagePartUiModel.Attachment.Audio(
        text = text,
        contentType = "audio/ogg",
        contentUri = "content://com.android.messaging.preview/message/audio.ogg".toUri(),
        width = 0,
        height = 0,
    )
}

internal fun previewFilePart(text: String?): ConversationMessagePartUiModel.Attachment.File {
    return ConversationMessagePartUiModel.Attachment.File(
        text = text,
        contentType = "application/pdf",
        contentUri = "content://com.android.messaging.preview/message/file.pdf".toUri(),
        width = 0,
        height = 0,
    )
}

internal fun previewVCardPart(): ConversationMessagePartUiModel.Attachment.VCard {
    return ConversationMessagePartUiModel.Attachment.VCard(
        text = null,
        contentType = "text/vcard",
        contentUri = "content://com.android.messaging.preview/message/contact.vcf".toUri(),
        width = 0,
        height = 0,
        vCardUiModel = previewVCardUiModel(),
    )
}

internal fun previewInlineAudioAttachment(): ConversationInlineAttachment.Audio {
    return ConversationInlineAttachment.Audio(
        key = "inline-audio",
        contentUri = "content://com.android.messaging.preview/message/audio.ogg",
        openAction = ConversationAttachmentOpenAction.OpenContent(
            contentType = "audio/ogg",
            contentUri = "content://com.android.messaging.preview/message/audio.ogg",
        ),
        titleText = "Voice note",
        titleTextResId = null,
    )
}

internal fun previewInlineFileAttachment(): ConversationInlineAttachment.File {
    return ConversationInlineAttachment.File(
        key = "inline-file",
        openAction = ConversationAttachmentOpenAction.OpenContent(
            contentType = "application/pdf",
            contentUri = "content://com.android.messaging.preview/message/file.pdf",
        ),
        subtitleTextResId = R.string.notification_file,
        titleText = "Quarterly report.pdf",
        titleTextResId = null,
    )
}

internal fun previewInlineVCardAttachment(
    type: ConversationVCardAttachmentType = ConversationVCardAttachmentType.CONTACT,
): ConversationInlineAttachment.VCard {
    val vCardUiModel = previewVCardUiModel(type = type)
    return ConversationInlineAttachment.VCard(
        key = "inline-vcard",
        contentUri = "content://com.android.messaging.preview/message/contact.vcf",
        openAction = ConversationAttachmentOpenAction.OpenContent(
            contentType = "text/vcard",
            contentUri = "content://com.android.messaging.preview/message/contact.vcf",
        ),
        type = type,
        avatarPhoto = vCardUiModel.avatarPhoto,
        normalizedDestination = vCardUiModel.normalizedDestination,
        titleText = vCardUiModel.titleText,
        titleTextResId = vCardUiModel.titleTextResId,
        subtitleText = vCardUiModel.subtitleText,
        subtitleTextResId = vCardUiModel.subtitleTextResId,
    )
}

internal fun previewMmsDownloadUiModel(
    state: MmsDownloadUiModel.State = MmsDownloadUiModel.State.AwaitingManualDownload,
): MmsDownloadUiModel {
    return MmsDownloadUiModel(
        state = state,
        sizeBytes = 2_400_000L,
        expiryTimestamp = PREVIEW_NOW_MILLIS + 86_400_000L,
    )
}

private fun previewMessage(
    messageId: String,
    text: String?,
    parts: ImmutableList<ConversationMessagePartUiModel>,
    status: ConversationMessageUiModel.Status,
    isIncoming: Boolean,
    senderDisplayName: String?,
    senderParticipantId: String?,
    selfParticipantId: String? = null,
    mmsDownload: MmsDownloadUiModel? = null,
    protocol: ConversationMessageUiModel.Protocol = ConversationMessageUiModel.Protocol.SMS,
    canDownloadMessage: Boolean = false,
    canSaveAttachments: Boolean = false,
): ConversationMessageUiModel {
    return ConversationMessageUiModel(
        messageId = messageId,
        conversationId = "conversation-1",
        text = text,
        parts = parts,
        sentTimestamp = PREVIEW_MESSAGE_RECEIVED_MILLIS,
        receivedTimestamp = PREVIEW_MESSAGE_RECEIVED_MILLIS,
        displayTimestamp = PREVIEW_MESSAGE_RECEIVED_MILLIS,
        status = status,
        isIncoming = isIncoming,
        senderDisplayName = senderDisplayName,
        senderAvatarUri = null,
        senderContactId = 1L,
        senderContactLookupKey = "preview-contact",
        senderNormalizedDestination = "+31622223333",
        senderParticipantId = senderParticipantId,
        selfParticipantId = selfParticipantId,
        canClusterWithPrevious = false,
        canClusterWithNext = false,
        canCopyMessageToClipboard = !text.isNullOrBlank(),
        canDownloadMessage = canDownloadMessage,
        canForwardMessage = true,
        canResendMessage = status == ConversationMessageUiModel.Status.Outgoing.Failed,
        canSaveAttachments = canSaveAttachments,
        isYouTubePreviewEnabled = true,
        mmsDownload = mmsDownload,
        mmsSubject = null,
        protocol = protocol,
    )
}
