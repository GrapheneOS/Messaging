package com.android.messaging.domain.shareintent.usecase

import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.IntentCompat
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.data.shareintent.repository.SharedAttachmentRepository
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface BuildSharedConversationDraft {
    suspend operator fun invoke(intent: Intent, caller: ComponentCaller): ConversationDraft?
}

internal class BuildSharedConversationDraftImpl @Inject constructor(
    private val resolveSharedContentType: ResolveSharedContentType,
    private val sharedAttachmentRepository: SharedAttachmentRepository,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : BuildSharedConversationDraft {

    override suspend fun invoke(
        intent: Intent,
        caller: ComponentCaller,
    ): ConversationDraft? {
        val subject = intent.sharedSubject()

        return withContext(ioDispatcher) {
            when (intent.action) {
                Intent.ACTION_SEND -> buildFromSend(intent, subject, caller)
                Intent.ACTION_SEND_MULTIPLE -> buildFromSendMultiple(intent, subject, caller)
                else -> null
            }
        }
    }

    private suspend fun buildFromSend(
        intent: Intent,
        subject: String,
        caller: ComponentCaller,
    ): ConversationDraft? {
        val contentUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )
        val contentType = resolveSharedContentType(contentUri, intent.type)

        return when {
            ContentType.TEXT_PLAIN == contentType -> {
                val messageText = intent.sharedMessageText()
                    .ifBlank { sharedTextFromContent(contentUri, caller).orEmpty() }

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
                    caller = caller,
                )

                draftOrNull(
                    messageText = intent.sharedMessageText(),
                    subject = subject,
                    attachments = listOfNotNull(attachment),
                )
            }

            else -> null
        }
    }

    private suspend fun buildFromSendMultiple(
        intent: Intent,
        subject: String,
        caller: ComponentCaller,
    ): ConversationDraft? {
        val sharedUris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        ).orEmpty()

        val attachments = sharedUris.mapNotNull { uri ->
            val contentType = resolveSharedContentType(uri, intent.type)
            if (!ContentType.isMediaType(contentType)) {
                return@mapNotNull null
            }
            persistAttachment(
                sourceUri = uri,
                contentType = contentType,
                caller = caller,
            )
        }

        return draftOrNull(
            messageText = intent.sharedMessageText(),
            subject = subject,
            attachments = attachments,
        )
    }

    private suspend fun sharedTextFromContent(
        uri: Uri?,
        caller: ComponentCaller,
    ): String? {
        if (uri == null) {
            return null
        }

        return when {
            caller.canReadContent(uri) -> {
                sharedAttachmentRepository.readTextContent(uri)
            }

            else -> {
                LogUtil.w(TAG, "Ignoring shared text without caller read permission")
                null
            }
        }
    }

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
        caller: ComponentCaller,
    ): ConversationDraftAttachment? {
        if (contentType.isNullOrBlank()) {
            return null
        }

        return when {
            caller.canReadContent(sourceUri) -> {
                sharedAttachmentRepository.persistToScratchSpace(sourceUri, contentType)
            }

            else -> {
                LogUtil.w(TAG, "Ignoring shared attachment without caller read permission")
                null
            }
        }
    }

    private fun ComponentCaller.canReadContent(uri: Uri): Boolean {
        return runCatching {
            checkContentUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            ) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    private companion object {
        private const val TAG = "BuildSharedDraft"
    }
}
