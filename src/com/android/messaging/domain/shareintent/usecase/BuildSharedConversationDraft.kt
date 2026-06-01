package com.android.messaging.domain.shareintent.usecase

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.data.conversation.model.draft.ConversationDraftAttachment
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface BuildSharedConversationDraft {
    suspend operator fun invoke(intent: Intent): ConversationDraft?
}

internal class BuildSharedConversationDraftImpl @Inject constructor(
    private val resolveSharedContentType: ResolveSharedContentType,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : BuildSharedConversationDraft {

    override suspend fun invoke(intent: Intent): ConversationDraft? {
        return withContext(ioDispatcher) {
            buildDraft(intent)
        }
    }

    private fun buildDraft(intent: Intent): ConversationDraft? {
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()

        return when (intent.action) {
            Intent.ACTION_SEND -> buildFromSend(intent, subject)
            Intent.ACTION_SEND_MULTIPLE -> buildFromSendMultiple(intent, subject)
            else -> null
        }
    }

    private fun buildFromSend(intent: Intent, subject: String): ConversationDraft? {
        val contentUri = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        )
        val contentType = resolveSharedContentType(contentUri, intent.type)

        return when {
            ContentType.TEXT_PLAIN == contentType -> {
                val messageText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                draftOrNull(messageText, subject, emptyList())
            }

            ContentType.isMediaType(contentType) && contentUri != null -> {
                val attachment = persistAttachment(contentUri, contentType)
                draftOrNull("", subject, listOfNotNull(attachment))
            }

            else -> null
        }
    }

    private fun buildFromSendMultiple(intent: Intent, subject: String): ConversationDraft? {
        if (!ContentType.isImageType(intent.type)) {
            return null
        }

        val imageUris = IntentCompat.getParcelableArrayListExtra(
            intent,
            Intent.EXTRA_STREAM,
            Uri::class.java,
        ).orEmpty()

        val attachments = imageUris.mapNotNull { uri ->
            persistAttachment(uri, resolveSharedContentType(uri, intent.type))
        }

        return draftOrNull("", subject, attachments)
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

    private fun persistAttachment(
        sourceUri: Uri,
        contentType: String?,
    ): ConversationDraftAttachment? {
        if (contentType.isNullOrBlank()) {
            return null
        }

        val persistedUri = UriUtil.persistContentToScratchSpace(sourceUri)

        return when (persistedUri) {
            null -> {
                LogUtil.w(TAG, "Failed to persist shared attachment to scratch space")
                null
            }

            else -> ConversationDraftAttachment(
                contentType = contentType,
                contentUri = persistedUri.toString(),
            )
        }
    }

    private companion object {
        private const val TAG = "BuildSharedConvDraft"
    }
}
