package com.android.messaging.ui.conversationlist.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.data.conversationlist.model.ConversationListMessageStatus
import com.android.messaging.ui.conversationlist.model.ConversationListItemUiModel
import com.android.messaging.ui.conversationlist.model.ConversationListPreviewUiModel
import com.android.messaging.util.Dates

@Composable
internal fun conversationListItemContentDescription(
    item: ConversationListItemUiModel,
): String {
    val message = conversationListItemContentDescriptionMessage(item)
    val time = remember(item.timestampMillis) {
        Dates.getConversationTimeString(item.timestampMillis).toString()
    }
    val spec = conversationListItemContentDescriptionSpec(
        item = item,
        message = message,
        time = time,
    )
    val primaryDescription = stringResource(
        id = spec.primaryResId,
        spec.senderOrConversationName,
        spec.message,
        spec.time,
        spec.conversationName,
    )
    val badgeDescriptions = conversationListItemBadgeContentDescriptionResIds(item)
        .map { resId -> stringResource(resId) }

    val messageDescription = when {
        spec.appendFailedDraftDescription -> {
            "$primaryDescription ${stringResource(R.string.failed_message_content_description)}"
        }

        else -> primaryDescription
    }

    if (badgeDescriptions.isEmpty()) {
        return messageDescription
    }

    return "$messageDescription ${badgeDescriptions.joinToString(separator = ". ")}."
}

internal fun conversationListItemContentDescriptionSpec(
    item: ConversationListItemUiModel,
    message: String,
    time: String,
): ConversationListItemContentDescription {
    val conversationName = item.title.orEmpty()
    val isDraft = item.snippet.isDraft || item.status == ConversationListMessageStatus.Draft
    val isOutgoing = item.isOutgoing || isDraft
    val senderOrConversationName = when {
        isOutgoing -> conversationName
        else -> item.snippet.senderName?.takeIf(String::isNotBlank) ?: conversationName
    }

    return ConversationListItemContentDescription(
        primaryResId = conversationListItemContentDescriptionResId(
            isGroup = item.avatar.isGroup,
            isOutgoing = isOutgoing,
            isDraft = isDraft,
            status = item.status,
        ),
        senderOrConversationName = senderOrConversationName,
        message = message,
        time = time,
        conversationName = conversationName,
        appendFailedDraftDescription = isOutgoing &&
            isDraft &&
            item.status is ConversationListMessageStatus.Failed,
    )
}

@Composable
private fun conversationListItemContentDescriptionMessage(
    item: ConversationListItemUiModel,
): String {
    val snippetText = item.snippet.text?.takeIf(String::isNotBlank)

    if (snippetText != null) {
        return snippetText
    }

    return when (item.snippet.preview) {
        is ConversationListPreviewUiModel.Audio -> {
            stringResource(R.string.conversation_list_snippet_audio_clip)
        }

        is ConversationListPreviewUiModel.Image -> {
            stringResource(R.string.conversation_list_snippet_picture)
        }

        is ConversationListPreviewUiModel.Video -> {
            stringResource(R.string.conversation_list_snippet_video)
        }

        is ConversationListPreviewUiModel.VCard -> {
            stringResource(R.string.conversation_list_snippet_vcard)
        }

        is ConversationListPreviewUiModel.File -> {
            stringResource(R.string.mms_text)
        }

        null -> ""
    }
}

internal fun conversationListItemBadgeContentDescriptionResIds(
    item: ConversationListItemUiModel,
): List<Int> {
    return buildList {
        if (item.isUnread) {
            add(R.string.conversation_list_status_unread)
        }

        if (item.isEnterprise) {
            add(R.string.conversation_list_status_work_profile)
        }

        when {
            item.isMuted -> {
                add(R.string.conversation_list_status_notifications_off)
            }

            item.isSnoozed -> {
                add(R.string.conversation_list_status_snoozed)
            }
        }

        if (item.isPinned) {
            add(R.string.conversation_list_status_pinned)
        }
    }
}

@StringRes
private fun conversationListItemContentDescriptionResId(
    isGroup: Boolean,
    isOutgoing: Boolean,
    isDraft: Boolean,
    status: ConversationListMessageStatus,
): Int {
    return when {
        isGroup && isOutgoing -> {
            groupOutgoingContentDescriptionResId(
                isDraft = isDraft,
                status = status,
            )
        }

        isGroup -> {
            groupIncomingContentDescriptionResId(status = status)
        }

        isOutgoing -> {
            oneOnOneOutgoingContentDescriptionResId(
                isDraft = isDraft,
                status = status,
            )
        }

        else -> {
            oneOnOneIncomingContentDescriptionResId(status = status)
        }
    }
}

@StringRes
private fun groupOutgoingContentDescriptionResId(
    isDraft: Boolean,
    status: ConversationListMessageStatus,
): Int {
    return when {
        isDraft -> {
            R.string.group_outgoing_draft_message_prefix
        }

        status == ConversationListMessageStatus.Sending -> {
            R.string.group_outgoing_sending_message_prefix
        }

        status is ConversationListMessageStatus.Failed -> {
            R.string.group_outgoing_failed_message_prefix
        }

        else -> {
            R.string.group_outgoing_successful_message_prefix
        }
    }
}

@StringRes
private fun groupIncomingContentDescriptionResId(
    status: ConversationListMessageStatus,
): Int {
    return when (status) {
        is ConversationListMessageStatus.Failed -> {
            R.string.group_incoming_failed_message_prefix
        }

        else -> {
            R.string.group_incoming_successful_message_prefix
        }
    }
}

@StringRes
private fun oneOnOneOutgoingContentDescriptionResId(
    isDraft: Boolean,
    status: ConversationListMessageStatus,
): Int {
    return when {
        isDraft -> {
            R.string.one_on_one_outgoing_draft_message_prefix
        }
        status == ConversationListMessageStatus.Sending -> {
            R.string.one_on_one_outgoing_sending_message_prefix
        }

        status is ConversationListMessageStatus.Failed -> {
            R.string.one_on_one_outgoing_failed_message_prefix
        }

        else -> {
            R.string.one_on_one_outgoing_successful_message_prefix
        }
    }
}

@StringRes
private fun oneOnOneIncomingContentDescriptionResId(
    status: ConversationListMessageStatus,
): Int {
    return when (status) {
        is ConversationListMessageStatus.Failed -> {
            R.string.one_on_one_incoming_failed_message_prefix
        }

        else -> {
            R.string.one_on_one_incoming_successful_message_prefix
        }
    }
}

internal data class ConversationListItemContentDescription(
    @param:StringRes val primaryResId: Int,
    val senderOrConversationName: String,
    val message: String,
    val time: String,
    val conversationName: String,
    val appendFailedDraftDescription: Boolean,
)
