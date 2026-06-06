package com.android.messaging.domain.shareintent.usecase

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepository
import com.android.messaging.util.ContentType
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList

internal interface BuildSharedConversationDraft {
    suspend operator fun invoke(intent: Intent): ConversationDraft?
}

internal class BuildSharedConversationDraftImpl @Inject constructor(
    private val resolveSharedContentType: ResolveSharedContentType,
    private val sharedAttachmentRepository: SharedAttachmentRepository,
) : BuildSharedConversationDraft {

    override suspend fun invoke(intent: Intent): ConversationDraft? {
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()

        return when (intent.action) {
            Intent.ACTION_SEND -> buildFromSend(intent, subject)
            Intent.ACTION_SEND_MULTIPLE -> buildFromSendMultiple(intent, subject)
            else -> null
        }
    }

    private suspend fun buildFromSend(
        intent: Intent,
        subject: String,
    ): ConversationDraft? {
        val contentUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )
        val contentType = resolveSharedContentType(contentUri, intent.type)

        return when {
            ContentType.TEXT_PLAIN == contentType -> {
                draftOrNull(
                    messageText = intent.sharedMessageText(),
                    subject = subject,
                    attachments = emptyList()
                )
            }

            ContentType.isMediaType(contentType) && contentUri != null -> {
                val attachment = persistAttachment(contentUri, contentType)
                draftOrNull(
                    messageText = intent.sharedMessageText(),
                    subject = subject,
                    attachments = listOfNotNull(attachment)
                )
            }

            else -> null
        }
    }

    private suspend fun buildFromSendMultiple(
        intent: Intent,
        subject: String,
    ): ConversationDraft? {
        if (!ContentType.isImageType(intent.type)) {
            return null
        }

        val imageUris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        ).orEmpty()

        val attachments = imageUris.mapNotNull { uri ->
            persistAttachment(
                sourceUri = uri,
                contentType = resolveSharedContentType(uri, intent.type)
            )
        }

        return draftOrNull(
            messageText = intent.sharedMessageText(),
            subject = subject,
            attachments = attachments
        )
    }

    private fun Intent.sharedMessageText(): String {
        return getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
    }

    private fun draftOrNull(
        messageText: String,
        subject: String,
        attachments: List<ConversationDraftAttachment>,
    ): ConversationDraft? {
        if (messageText.isBlank() && subject.isBlank() && attachments.isEmpty()) {
            return null
        }

        return ConversationDraft(
            messageText = messageText,
            subjectText = subject,
            attachments = attachments.toImmutableList(),
        )
    }

    private suspend fun persistAttachment(
        sourceUri: Uri,
        contentType: String?,
    ): ConversationDraftAttachment? {
        if (contentType.isNullOrBlank()) {
            return null
        }

        return sharedAttachmentRepository.persistToScratchSpace(sourceUri, contentType)
    }
}
