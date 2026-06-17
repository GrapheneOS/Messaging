package com.android.messaging.domain.shareintent.usecase

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.shareintent.model.SharedTextContentResult
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepository
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.shareintent.model.SharedConversationDraftResult
import com.android.messaging.util.ContentType
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface BuildSharedConversationDraft {
    suspend operator fun invoke(intent: Intent): SharedConversationDraftResult
}

internal class BuildSharedConversationDraftImpl @Inject constructor(
    private val resolveSharedContentType: ResolveSharedContentType,
    private val sharedAttachmentRepository: SharedAttachmentRepository,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : BuildSharedConversationDraft {

    override suspend fun invoke(intent: Intent): SharedConversationDraftResult {
        val subject = intent.sharedSubject()

        return withContext(ioDispatcher) {
            when (intent.action) {
                Intent.ACTION_SEND -> buildFromSend(intent, subject)
                Intent.ACTION_SEND_MULTIPLE -> buildFromSendMultiple(intent, subject)
                else -> SharedConversationDraftResult(
                    draft = null,
                    hasDroppedContent = false,
                )
            }
        }
    }

    private suspend fun buildFromSend(
        intent: Intent,
        subject: String,
    ): SharedConversationDraftResult {
        val sharedUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )

        val contentUri = sharedUri?.takeUnless { UriUtil.isFileUri(it) }
        val contentType = resolveSharedContentType(contentUri, intent.type)

        var hasDroppedContent = sharedUri != null && contentUri == null
        val draft = when {
            ContentType.TEXT_PLAIN == contentType -> {
                var messageText = intent.sharedMessageText()
                if (messageText.isBlank()) {
                    val sharedText = readSharedText(contentUri)
                    messageText = sharedText.text.orEmpty()
                    hasDroppedContent = hasDroppedContent || sharedText.hasDroppedContent
                }

                draftOrNull(
                    messageText = messageText,
                    subject = subject,
                    attachments = emptyList(),
                )
            }

            ContentType.isMediaType(contentType) && contentUri != null -> {
                val attachment = persistAttachment(
                    sourceUri = contentUri,
                    contentType = contentType,
                )

                if (attachment == null) {
                    hasDroppedContent = true
                }

                draftOrNull(
                    messageText = intent.sharedMessageText(),
                    subject = subject,
                    attachments = listOfNotNull(attachment),
                )
            }

            else -> {
                if (contentUri != null) {
                    hasDroppedContent = true
                }

                draftOrNull(
                    messageText = intent.sharedMessageText(),
                    subject = subject,
                    attachments = emptyList(),
                )
            }
        }

        return SharedConversationDraftResult(
            draft = draft,
            hasDroppedContent = hasDroppedContent,
        )
    }

    private suspend fun buildFromSendMultiple(
        intent: Intent,
        subject: String,
    ): SharedConversationDraftResult {
        val sharedUris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        ).orEmpty()

        val attachments = mutableListOf<ConversationDraftAttachment>()
        val sharedTexts = mutableListOf<String>()

        val (allowedUris, rejectedUris) = sharedUris.partition { !UriUtil.isFileUri(it) }

        var hasDroppedContent = rejectedUris.isNotEmpty()
        allowedUris.forEach { uri ->
            val contentType = resolveSharedContentType(uri, intent.type)
            when {
                ContentType.TEXT_PLAIN == contentType -> {
                    val sharedText = readSharedText(uri)
                    sharedText.text?.let(sharedTexts::add)

                    if (sharedText.hasDroppedContent) {
                        hasDroppedContent = true
                    }
                }

                ContentType.isMediaType(contentType) -> {
                    val attachment = persistAttachment(
                        sourceUri = uri,
                        contentType = contentType,
                    )

                    when (attachment) {
                        null -> hasDroppedContent = true
                        else -> attachments.add(attachment)
                    }
                }

                else -> {
                    hasDroppedContent = true
                }
            }
        }

        val messageText = buildList {
            intent.sharedMessageText().takeIf(String::isNotBlank)?.let(::add)
            addAll(sharedTexts)
        }.joinToString(separator = "\n")

        return SharedConversationDraftResult(
            draft = draftOrNull(
                messageText = messageText,
                subject = subject,
                attachments = attachments,
            ),
            hasDroppedContent = hasDroppedContent,
        )
    }

    private suspend fun readSharedText(uri: Uri?): SharedTextRead {
        if (uri == null) {
            return SharedTextRead()
        }

        return when (val result = sharedAttachmentRepository.readTextContent(uri)) {
            SharedTextContentResult.Empty -> SharedTextRead()
            SharedTextContentResult.Failed -> SharedTextRead(hasDroppedContent = true)
            is SharedTextContentResult.Read -> SharedTextRead(text = result.text)
        }
    }

    private data class SharedTextRead(
        val text: String? = null,
        val hasDroppedContent: Boolean = false,
    )

    private fun Intent.sharedSubject(): String {
        return (getStringExtra(Intent.EXTRA_SUBJECT) ?: getStringExtra(Intent.EXTRA_TITLE))
            .orEmpty()
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
